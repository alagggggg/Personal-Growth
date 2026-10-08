package com.nhatkycaitien.app;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Excel-only backup and restore. BackupData is hidden and is the authoritative restore payload. */
public final class BackupExcel {
    private static final int CELL_LIMIT = 32000;
    private static final long FILE_LIMIT = 300L * 1024L * 1024L;
    private BackupExcel() {}

    public static byte[] create(String json, String version) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray cases = root.optJSONArray("cases");
        if (cases == null) cases = new JSONArray();
        return workbook(root.toString(), cases, version);
    }

    public static String extractDataJson(byte[] xlsx) throws Exception {
        if (xlsx == null || xlsx.length < 4 || xlsx[0] != 'P' || xlsx[1] != 'K')
            throw new Exception("Tệp không phải Excel .xlsx hợp lệ");
        String backupSheet = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("xl/worksheets/sheet5.xml".equals(entry.getName())) {
                    backupSheet = new String(read(zip, FILE_LIMIT), StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        if (backupSheet == null) throw new Exception("Không tìm thấy dữ liệu khôi phục trong Excel");
        Matcher matcher = Pattern.compile("<t(?:\\s+[^>]*)?>(.*?)</t>", Pattern.DOTALL).matcher(backupSheet);
        StringBuilder payload = new StringBuilder();
        boolean first = true;
        while (matcher.find()) {
            String value = unescape(matcher.group(1));
            if (first) { first = false; continue; }
            payload.append(value);
        }
        if (payload.length() == 0) throw new Exception("Sheet BackupData không có dữ liệu");
        String json;
        try { json = new String(Base64.decode(payload.toString(), Base64.DEFAULT), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new Exception("Dữ liệu sao lưu trong Excel bị hỏng"); }
        JSONObject root = new JSONObject(json);
        if (root.optJSONArray("cases") == null) throw new Exception("Excel không có danh sách hồ sơ hợp lệ");
        return root.toString();
    }

    private static byte[] workbook(String fullJson, JSONArray cases, String version) throws Exception {
        List<Row> records = new ArrayList<>();
        List<Row> updates = new ArrayList<>();
        List<Row> solutions = new ArrayList<>();
        List<Row> finals = new ArrayList<>();
        List<ImageItem> images = new ArrayList<>();
        int maxPhotos = 0;

        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.optJSONObject(i); if (c == null) continue;
            String id = c.optString("id", ""), title = c.optString("title", "");
            JSONArray photos = c.optJSONArray("photos");
            List<String> values = new ArrayList<>();
            if (photos != null) for (int j=0;j<photos.length();j++) values.add(photos.optString(j,""));
            if (values.isEmpty() && !c.optString("photo","").isEmpty()) values.add(c.optString("photo",""));
            maxPhotos = Math.max(maxPhotos, values.size());
            JSONObject safe = new JSONObject(c.toString()); safe.remove("photos"); safe.remove("photo");
            JSONArray impacts = c.optJSONArray("impacts"), us=c.optJSONArray("updates"), ss=c.optJSONArray("solutions");
            JSONObject f=c.optJSONObject("final");
            Row row = new Row();
            row.add(id,title,c.optString("type"),c.optString("category"),String.valueOf(c.optInt("severity",0)),impacts==null?"[]":impacts.toString(),c.optString("visualSeverity"),c.optString("status"),c.optString("startedAt"),c.optString("createdAt"),c.optString("updatedAt"),c.optString("closedAt"),c.optString("owner"),c.optString("location"),c.optString("description"),us==null?"[]":us.toString(),ss==null?"[]":ss.toString(),f==null?"":f.toString(),safe.toString());
            int excelRow = records.size() + 2;
            for (int j=0;j<values.size();j++) {
                try { images.add(decodeImage(values.get(j), excelRow, 19+j)); }
                catch (Exception ignored) { /* One damaged image must not block the backup. */ }
            }
            records.add(row);
            if(us!=null)for(int j=0;j<us.length();j++){JSONObject u=us.optJSONObject(j);if(u!=null)updates.add(new Row(id,String.valueOf(j+1),u.optString("type"),first(u,"content","text","description"),first(u,"at","timestamp","createdAt","date"),String.valueOf(u.optInt("minutes",u.optInt("duration",0))),String.valueOf(u.optDouble("cost",0)),u.toString()));}
            if(ss!=null)for(int j=0;j<ss.length();j++){JSONObject s=ss.optJSONObject(j);if(s!=null)solutions.add(new Row(id,String.valueOf(j+1),first(s,"name","title"),first(s,"description","content"),first(s,"startDate","startedAt"),first(s,"reviewDate","evaluationDate"),first(s,"successCriteria","criteria"),s.toString()));}
            if(f!=null)finals.add(new Row(id,first(f,"result","actualResult"),first(f,"good","strengths"),first(f,"bad","improvements"),first(f,"lesson","lessons"),first(f,"evaluation","assessment"),String.valueOf(f.optInt("effectiveness",0)),first(f,"decision","finalDecision"),c.optString("closedAt"),f.toString()));
        }

        List<String> headers = new ArrayList<>();
        String[] base={"Case ID","Tiêu đề","Loại","Nhóm","Mức độ","Ảnh hưởng ban đầu","Mức trực quan","Trạng thái","Bắt đầu","Tạo lúc","Cập nhật lúc","Đóng lúc","Người liên quan","Địa điểm","Mô tả","Cập nhật JSON","Phương án JSON","Kết quả cuối JSON","Toàn bộ hồ sơ JSON"};
        for(String h:base)headers.add(h); for(int i=1;i<=maxPhotos;i++)headers.add("Ảnh "+i);
        for(Row r:records)for(int i=0;i<maxPhotos;i++)r.values.add(i<imageCountForRow(images,records.indexOf(r)+2)?"Ảnh được nhúng":"");

        String payload=Base64.encodeToString(fullJson.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
        List<Row> backup=new ArrayList<>();
        for(int i=0;i<payload.length();i+=CELL_LIMIT)backup.add(new Row(payload.substring(i,Math.min(payload.length(),i+CELL_LIMIT))));

        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(output,StandardCharsets.UTF_8)){
            text(zip,"[Content_Types].xml",contentTypes(!images.isEmpty()));
            text(zip,"_rels/.rels",rootRels());
            text(zip,"docProps/core.xml",core(version));
            text(zip,"xl/workbook.xml",workbookXml());
            text(zip,"xl/_rels/workbook.xml.rels",workbookRels());
            text(zip,"xl/styles.xml",styles());
            text(zip,"xl/worksheets/sheet1.xml",sheet(headers,records,true,!images.isEmpty()));
            text(zip,"xl/worksheets/sheet2.xml",sheet(list("Case ID","STT","Loại","Nội dung","Thời gian","Phút","Chi phí","Toàn bộ cập nhật JSON"),updates,false,false));
            text(zip,"xl/worksheets/sheet3.xml",sheet(list("Case ID","STT","Tên","Mô tả","Ngày bắt đầu","Ngày đánh giá","Tiêu chí thành công","Toàn bộ phương án JSON"),solutions,false,false));
            text(zip,"xl/worksheets/sheet4.xml",sheet(list("Case ID","Kết quả thực tế","Điểm tốt","Điểm cần thay đổi","Bài học","Đánh giá","Hiệu quả","Quyết định","Ngày đóng","Toàn bộ kết quả JSON"),finals,false,false));
            text(zip,"xl/worksheets/sheet5.xml",sheet(list("BackupData - Không chỉnh sửa"),backup,false,false));
            if(!images.isEmpty()){
                text(zip,"xl/worksheets/_rels/sheet1.xml.rels",sheetDrawingRel());
                text(zip,"xl/drawings/drawing1.xml",drawing(images));
                text(zip,"xl/drawings/_rels/drawing1.xml.rels",drawingRels(images));
                for(int i=0;i<images.size();i++)put(zip,"xl/media/image"+(i+1)+"."+images.get(i).ext,images.get(i).bytes);
            }
        }
        return output.toByteArray();
    }

    private static int imageCountForRow(List<ImageItem> images,int row){int n=0;for(ImageItem i:images)if(i.row==row)n++;return n;}
    private static ImageItem decodeImage(String data,int row,int col)throws Exception{if(data==null||!data.startsWith("data:image/"))throw new Exception("Ảnh không phải Base64");int comma=data.indexOf(',');if(comma<0)throw new Exception("Ảnh thiếu dữ liệu");String mime=data.substring(5,comma).toLowerCase();String ext=mime.contains("png")?"png":"jpg";byte[] bytes=Base64.decode(data.substring(comma+1),Base64.DEFAULT);if(bytes.length<16)throw new Exception("Ảnh không hợp lệ");return new ImageItem(row,col,ext,bytes);}
    private static String first(JSONObject o,String...keys){for(String k:keys){String v=o.optString(k,"");if(!v.isEmpty())return v;}return "";}
    private static List<String> list(String...v){List<String>x=new ArrayList<>();for(String s:v)x.add(s);return x;}
    private static String sheet(List<String> headers,List<Row> rows,boolean imageRows,boolean drawing){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" state=\"frozen\"/></sheetView></sheetViews><cols>");for(int i=1;i<=headers.size();i++)b.append("<col min=\"").append(i).append("\" max=\"").append(i).append("\" width=\"").append(i>=20?20:22).append("\" customWidth=\"1\"/>");b.append("</cols><sheetData>");writeRow(b,1,headers,true,false);for(int r=0;r<rows.size();r++)writeRow(b,r+2,rows.get(r).values,false,imageRows);b.append("</sheetData><autoFilter ref=\"A1:").append(col(headers.size())).append(rows.size()+1).append("\"/>");if(drawing)b.append("<drawing r:id=\"rId1\"/>");return b.append("</worksheet>").toString();}
    private static void writeRow(StringBuilder b,int row,List<String> values,boolean header,boolean tall){b.append("<row r=\"").append(row).append("\"");if(tall)b.append(" ht=\"88\" customHeight=\"1\"");b.append(">");for(int c=0;c<values.size();c++){String v=cell(values.get(c));b.append("<c r=\"").append(col(c+1)).append(row).append("\" t=\"inlineStr\"").append(header?" s=\"1\"":"").append("><is><t xml:space=\"preserve\">").append(esc(v)).append("</t></is></c>");}b.append("</row>");}
    private static String workbookXml(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Full Records\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"Updates\" sheetId=\"2\" r:id=\"rId2\"/><sheet name=\"Solutions\" sheetId=\"3\" r:id=\"rId3\"/><sheet name=\"Final Results\" sheetId=\"4\" r:id=\"rId4\"/><sheet name=\"BackupData\" sheetId=\"5\" state=\"hidden\" r:id=\"rId5\"/></sheets></workbook>";}
    private static String workbookRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/><Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/><Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet4.xml\"/><Relationship Id=\"rId5\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet5.xml\"/><Relationship Id=\"rId6\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>";}
    private static String contentTypes(boolean drawing){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Default Extension=\"jpg\" ContentType=\"image/jpeg\"/><Default Extension=\"png\" ContentType=\"image/png\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/><Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/worksheets/sheet4.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/worksheets/sheet5.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"+(drawing?"<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>":"")+"</Types>";}
    private static String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/></Relationships>";}
    private static String core(String version){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:title>Personal Growth "+esc(version)+" Backup</dc:title></cp:coreProperties>";}
    private static String styles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"10\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1477EA\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"2\"><xf/><xf fontId=\"1\" fillId=\"2\" applyFont=\"1\" applyFill=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";}
    private static String sheetDrawingRel(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>";}
    private static String drawingRels(List<ImageItem> images){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=0;i<images.size();i++)b.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image").append(i+1).append('.').append(images.get(i).ext).append("\"/>");return b.append("</Relationships>").toString();}
    private static String drawing(List<ImageItem> images){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");for(int i=0;i<images.size();i++){ImageItem x=images.get(i);b.append("<xdr:oneCellAnchor><xdr:from><xdr:col>").append(x.col).append("</xdr:col><xdr:colOff>9525</xdr:colOff><xdr:row>").append(x.row-1).append("</xdr:row><xdr:rowOff>9525</xdr:rowOff></xdr:from><xdr:ext cx=\"1333500\" cy=\"952500\"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(i+1).append("\" name=\"Photo ").append(i+1).append("\"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed=\"rId").append(i+1).append("\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>");}return b.append("</xdr:wsDr>").toString();}
    private static String cell(String s){if(s==null)return"";return s.length()<=32767?s:s.substring(0,32700)+" [Đã rút gọn]";}
    private static String col(int n){StringBuilder b=new StringBuilder();while(n>0){n--;b.insert(0,(char)('A'+n%26));n/=26;}return b.toString();}
    private static String esc(String x){if(x==null)return"";return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static String unescape(String x){return x.replace("&quot;","\"").replace("&gt;",">").replace("&lt;","<").replace("&amp;","&");}
    private static byte[] read(InputStream in,long limit)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0){if(o.size()+n>limit)throw new Exception("Tệp Excel quá lớn");o.write(b,0,n);}return o.toByteArray();}
    private static void put(ZipOutputStream z,String name,byte[] data)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(data);z.closeEntry();}
    private static void text(ZipOutputStream z,String name,String data)throws Exception{put(z,name,data.getBytes(StandardCharsets.UTF_8));}
    private static final class Row{final List<String> values=new ArrayList<>();Row(String...v){add(v);}void add(String...v){for(String x:v)values.add(x);}}
    private static final class ImageItem{final int row,col;final String ext;final byte[] bytes;ImageItem(int r,int c,String e,byte[] b){row=r;col=c;ext=e;bytes=b;}}
}
