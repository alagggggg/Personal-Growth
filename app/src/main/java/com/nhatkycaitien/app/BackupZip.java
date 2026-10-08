package com.nhatkycaitien.app;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class BackupZip {
    private static final long PACKAGE_LIMIT = 250L * 1024L * 1024L;
    private static final long ENTRY_LIMIT = 80L * 1024L * 1024L;

    private BackupZip() {}

    public static byte[] create(String json, String version) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray cases = root.optJSONArray("cases");
        if (cases == null) cases = new JSONArray();
        JSONObject manifest = new JSONObject()
            .put("backupFormat", "personal-growth-backup")
            .put("backupVersion", 3)
            .put("appVersion", version)
            .put("exportedAt", root.optString("exportedAt", ""))
            .put("caseCount", cases.length())
            .put("primaryData", "data.json")
            .put("reportFile", "report.xlsx")
            .put("restoreRule", "JSON_PRIMARY_EXCEL_WITH_EMBEDDED_PREVIEWS");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            put(zip, "manifest.json", manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            put(zip, "data.json", root.toString(2).getBytes(StandardCharsets.UTF_8));
            put(zip, "report.xlsx", createWorkbook(cases));
        }
        return output.toByteArray();
    }

    public static String extractDataJson(byte[] bytes) throws Exception {
        String data = null;
        boolean manifestFound = false;
        boolean excelFound = false;
        long total = 0;
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("../") || !names.add(name))
                    throw new Exception("Cấu trúc ZIP không an toàn");
                if (entry.isDirectory()) continue;
                byte[] content = read(zip, ENTRY_LIMIT);
                total += content.length;
                if (total > PACKAGE_LIMIT) throw new Exception("Gói sao lưu quá lớn");
                if ("manifest.json".equals(name)) {
                    JSONObject manifest = new JSONObject(new String(content, StandardCharsets.UTF_8));
                    if (!"personal-growth-backup".equals(manifest.optString("backupFormat")))
                        throw new Exception("Không đúng gói Personal Growth");
                    manifestFound = true;
                } else if ("data.json".equals(name)) {
                    data = new String(content, StandardCharsets.UTF_8);
                } else if ("report.xlsx".equals(name)) {
                    if (content.length < 4 || content[0] != 'P' || content[1] != 'K')
                        throw new Exception("report.xlsx không hợp lệ");
                    excelFound = true;
                }
            }
        }
        if (!manifestFound || data == null || !excelFound)
            throw new Exception("ZIP phải có manifest.json, data.json và report.xlsx");
        JSONObject root = new JSONObject(data);
        if (root.optJSONArray("cases") == null) throw new Exception("data.json thiếu danh sách hồ sơ");
        return root.toString();
    }

    private static byte[] createWorkbook(JSONArray cases) throws Exception {
        List<Sheet> sheets = new ArrayList<>();
        Sheet full = new Sheet("Full Records", new String[]{"Case ID","Tiêu đề","Loại","Nhóm","Mức độ","Ảnh hưởng ban đầu","Mức trực quan","Trạng thái","Bắt đầu","Tạo lúc","Cập nhật lúc","Đóng lúc","Người liên quan","Địa điểm","Mô tả","Thông tin ảnh","Cập nhật JSON","Phương án JSON","Kết quả cuối JSON","Toàn bộ hồ sơ JSON"});
        Sheet updates = new Sheet("Updates", new String[]{"Case ID","STT","Loại","Nội dung","Thời gian","Phút","Chi phí","Toàn bộ cập nhật JSON"});
        Sheet solutions = new Sheet("Solutions", new String[]{"Case ID","STT","Tên","Mô tả","Ngày bắt đầu","Ngày đánh giá","Tiêu chí thành công","Toàn bộ phương án JSON"});
        Sheet finals = new Sheet("Final Results", new String[]{"Case ID","Kết quả thực tế","Điểm tốt","Điểm cần thay đổi","Bài học","Đánh giá","Hiệu quả","Quyết định","Ngày đóng","Toàn bộ kết quả JSON"});
        Sheet photos = new Sheet("Photos", new String[]{"Case ID","Tiêu đề hồ sơ","STT ảnh","Ảnh xem trước","Trạng thái"});
        List<Photo> embedded = new ArrayList<>();

        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.optJSONObject(i);
            if (c == null) continue;
            String id = c.optString("id", "");
            String title = c.optString("title", "");
            JSONArray us = c.optJSONArray("updates");
            JSONArray ss = c.optJSONArray("solutions");
            JSONArray ps = c.optJSONArray("photos");
            JSONArray impacts = c.optJSONArray("impacts");
            JSONObject f = c.optJSONObject("final");
            JSONObject excelRecord = new JSONObject(c.toString());
            excelRecord.remove("photos");
            excelRecord.remove("photo");
            full.add(id,title,c.optString("type"),c.optString("category"),String.valueOf(c.optInt("severity",0)),impacts==null?"[]":impacts.toString(),c.optString("visualSeverity"),c.optString("status"),c.optString("startedAt"),c.optString("createdAt"),c.optString("updatedAt"),c.optString("closedAt"),c.optString("owner"),c.optString("location"),c.optString("description"),photoSummary(ps,c.optString("photo","")),us==null?"[]":us.toString(),ss==null?"[]":ss.toString(),f==null?"":f.toString(),excelRecord.toString());

            if (us != null) for (int j=0;j<us.length();j++) {
                JSONObject x=us.optJSONObject(j); if(x==null)continue;
                updates.add(id,String.valueOf(j+1),x.optString("type"),first(x,"content","text","description"),first(x,"at","timestamp","createdAt","date"),String.valueOf(x.optInt("minutes",x.optInt("duration",0))),String.valueOf(x.optDouble("cost",0)),x.toString());
            }
            if (ss != null) for (int j=0;j<ss.length();j++) {
                JSONObject x=ss.optJSONObject(j); if(x==null)continue;
                solutions.add(id,String.valueOf(j+1),first(x,"name","title"),first(x,"description","content"),first(x,"startDate","startedAt"),first(x,"reviewDate","evaluationDate"),first(x,"successCriteria","criteria"),x.toString());
            }
            if (f != null) finals.add(id,first(f,"result","actualResult"),first(f,"good","strengths"),first(f,"bad","improvements"),first(f,"lesson","lessons"),first(f,"evaluation","assessment"),String.valueOf(f.optInt("effectiveness",0)),first(f,"decision","finalDecision"),c.optString("closedAt"),f.toString());

            List<String> imageValues = new ArrayList<>();
            if (ps != null) for (int j=0;j<ps.length();j++) imageValues.add(ps.optString(j,""));
            if (imageValues.isEmpty() && !c.optString("photo","").isEmpty()) imageValues.add(c.optString("photo",""));
            for (int j=0;j<imageValues.size();j++) {
                Photo photo = decodePhoto(imageValues.get(j), id, title, j+1, photos.rows.size()+1);
                embedded.add(photo);
                photos.add(id,title,String.valueOf(j+1),photo.valid?"Ảnh được nhúng ở cột D":"Không có ảnh",photo.status);
            }
        }
        sheets.add(full); sheets.add(updates); sheets.add(solutions); sheets.add(finals); sheets.add(photos);
        return writeWorkbook(sheets, embedded);
    }

    private static Photo decodePhoto(String value,String caseId,String title,int index,int excelRow) {
        Photo p = new Photo(caseId,title,index,excelRow);
        try {
            if (value == null || !value.startsWith("data:image/")) {
                p.status = "Không phải ảnh Base64 được nhúng";
                return p;
            }
            int comma=value.indexOf(',');
            if(comma<0)throw new Exception("Thiếu dữ liệu ảnh");
            String mime=value.substring(5,comma).toLowerCase();
            if(!mime.contains("jpeg")&&!mime.contains("jpg")&&!mime.contains("png"))throw new Exception("Định dạng chưa hỗ trợ");
            p.extension=mime.contains("png")?"png":"jpg";
            p.mime=p.extension.equals("png")?"image/png":"image/jpeg";
            p.bytes=Base64.decode(value.substring(comma+1),Base64.DEFAULT);
            if(p.bytes.length<16)throw new Exception("Ảnh quá nhỏ hoặc hỏng");
            p.valid=true;p.status="Đã nhúng "+p.extension.toUpperCase()+" · "+p.bytes.length+" bytes";
        } catch(Exception e) { p.valid=false;p.status="Lỗi ảnh: "+e.getMessage(); }
        return p;
    }

    private static byte[] writeWorkbook(List<Sheet> sheets,List<Photo> photos)throws Exception {
        List<Photo> valid=new ArrayList<>();for(Photo p:photos)if(p.valid)valid.add(p);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){
            text(zip,"[Content_Types].xml",contentTypes(sheets.size(),!valid.isEmpty()));
            text(zip,"_rels/.rels",rootRels());
            text(zip,"xl/workbook.xml",workbook(sheets));
            text(zip,"xl/_rels/workbook.xml.rels",workbookRels(sheets.size()));
            text(zip,"xl/styles.xml",styles());
            for(int i=0;i<sheets.size();i++) text(zip,"xl/worksheets/sheet"+(i+1)+".xml",sheetXml(sheets.get(i),i==4&&!valid.isEmpty()));
            if(!valid.isEmpty()){
                text(zip,"xl/worksheets/_rels/sheet5.xml.rels",sheetDrawingRel());
                text(zip,"xl/drawings/drawing1.xml",drawingXml(valid));
                text(zip,"xl/drawings/_rels/drawing1.xml.rels",drawingRels(valid));
                for(int i=0;i<valid.size();i++)put(zip,"xl/media/image"+(i+1)+"."+valid.get(i).extension,valid.get(i).bytes);
            }
        }
        return out.toByteArray();
    }

    private static String contentTypes(int n,boolean drawing){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Default Extension=\"jpg\" ContentType=\"image/jpeg\"/><Default Extension=\"png\" ContentType=\"image/png\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");for(int i=1;i<=n;i++)b.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");if(drawing)b.append("<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");return b.append("</Types>").toString();}
    private static String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";}
    private static String workbook(List<Sheet>s){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");for(int i=0;i<s.size();i++)b.append("<sheet name=\"").append(esc(s.get(i).name)).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");return b.append("</sheets></workbook>").toString();}
    private static String workbookRels(int n){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=1;i<=n;i++)b.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");return b.append("<Relationship Id=\"rId").append(n+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>").toString();}
    private static String styles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"10\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1477EA\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"2\"><xf/><xf fontId=\"1\" fillId=\"2\" applyFont=\"1\" applyFill=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";}
    private static String sheetXml(Sheet s,boolean drawing){int cols=s.rows.get(0).length;StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" state=\"frozen\"/></sheetView></sheetViews><cols>");for(int i=1;i<=cols;i++)b.append("<col min=\"").append(i).append("\" max=\"").append(i).append("\" width=\"").append(s.name.equals("Photos")&&i==4?24:(i>15?45:22)).append("\" customWidth=\"1\"/>");b.append("</cols><sheetData>");for(int r=0;r<s.rows.size();r++){b.append("<row r=\"").append(r+1).append("\"");if(s.name.equals("Photos")&&r>0)b.append(" ht=\"82\" customHeight=\"1\"");b.append(">");String[] x=s.rows.get(r);for(int c=0;c<x.length;c++)b.append("<c r=\"").append(col(c+1)).append(r+1).append("\" t=\"inlineStr\"").append(r==0?" s=\"1\"":"").append("><is><t xml:space=\"preserve\">").append(esc(cell(x[c]))).append("</t></is></c>");b.append("</row>");}b.append("</sheetData><autoFilter ref=\"A1:").append(col(cols)).append(s.rows.size()).append("\"/>");if(drawing)b.append("<drawing r:id=\"rId1\"/>");return b.append("</worksheet>").toString();}
    private static String sheetDrawingRel(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>";}
    private static String drawingRels(List<Photo> p){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=0;i<p.size();i++)b.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image").append(i+1).append('.').append(p.get(i).extension).append("\"/>");return b.append("</Relationships>").toString();}
    private static String drawingXml(List<Photo> p){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");for(int i=0;i<p.size();i++){int row=p.get(i).excelRow-1;b.append("<xdr:oneCellAnchor><xdr:from><xdr:col>3</xdr:col><xdr:colOff>9525</xdr:colOff><xdr:row>").append(row).append("</xdr:row><xdr:rowOff>9525</xdr:rowOff></xdr:from><xdr:ext cx=\"1333500\" cy=\"952500\"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(i+1).append("\" name=\"Photo ").append(i+1).append("\"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed=\"rId").append(i+1).append("\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"1333500\" cy=\"952500\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>");}return b.append("</xdr:wsDr>").toString();}

    private static String first(JSONObject o,String...keys){for(String k:keys){String v=o.optString(k,"");if(!v.isEmpty())return v;}return "";}
    private static String photoSummary(JSONArray p,String legacy){int n=p==null?0:p.length();if(n==0&&!legacy.isEmpty())n=1;return "Số ảnh: "+n+". Ảnh xem trước nằm trong sheet Photos; ảnh gốc nằm trong data.json";}
    private static String cell(String value){if(value==null)return"";return value.length()<=32767?value:value.substring(0,32700)+" [Đã rút gọn; bản đầy đủ nằm trong data.json]";}
    private static String col(int n){StringBuilder b=new StringBuilder();while(n>0){n--;b.insert(0,(char)('A'+n%26));n/=26;}return b.toString();}
    private static String esc(String x){if(x==null)return"";return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static byte[] read(InputStream input,long limit)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))>0){if(out.size()+n>limit)throw new Exception("Tệp trong ZIP quá lớn");out.write(buffer,0,n);}return out.toByteArray();}
    private static void put(ZipOutputStream zip,String name,byte[] data)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();}
    private static void text(ZipOutputStream zip,String name,String data)throws Exception{put(zip,name,data.getBytes(StandardCharsets.UTF_8));}
    private static final class Sheet{final String name;final List<String[]> rows=new ArrayList<>();Sheet(String n,String[] headers){name=n;rows.add(headers);}void add(String...values){rows.add(values);}}
    private static final class Photo{final String caseId,title;final int index,excelRow;boolean valid;String extension="jpg",mime="image/jpeg",status="";byte[] bytes=new byte[0];Photo(String c,String t,int i,int r){caseId=c;title=t;index=i;excelRow=r;}}
}
