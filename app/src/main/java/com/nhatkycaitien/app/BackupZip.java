package com.nhatkycaitien.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

/** Creates and reads the portable Personal Growth backup package. */
public final class BackupZip {
    private static final long MAX_ENTRY_BYTES = 80L * 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 250L * 1024L * 1024L;
    private BackupZip() {}

    public static byte[] create(String dataJson, String appVersion) throws Exception {
        JSONObject root = new JSONObject(dataJson == null ? "{}" : dataJson);
        JSONArray cases = root.optJSONArray("cases");
        if (cases == null) cases = new JSONArray();
        byte[] excel = createExcel(cases);
        JSONObject manifest = new JSONObject();
        manifest.put("backupFormat", "personal-growth-backup");
        manifest.put("backupVersion", 1);
        manifest.put("appVersion", appVersion);
        manifest.put("exportedAt", root.optString("exportedAt", ""));
        manifest.put("caseCount", cases.length());
        manifest.put("primaryData", "data.json");
        manifest.put("reportFile", "report.xlsx");
        manifest.put("restoreRule", "JSON_PRIMARY_EXCEL_REPORT");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "manifest.json", manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            put(zip, "data.json", root.toString(2).getBytes(StandardCharsets.UTF_8));
            put(zip, "report.xlsx", excel);
        }
        return bytes.toByteArray();
    }

    public static String extractDataJson(byte[] packageBytes) throws Exception {
        if (packageBytes == null || packageBytes.length < 4) throw new Exception("Tệp sao lưu trống");
        String data = null;
        boolean hasManifest = false, hasExcel = false;
        long total = 0;
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(packageBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = safeName(entry.getName());
                if (entry.isDirectory()) continue;
                if (!names.add(name)) throw new Exception("Gói sao lưu có tệp trùng tên");
                byte[] content = readEntry(zip, MAX_ENTRY_BYTES);
                total += content.length;
                if (total > MAX_TOTAL_BYTES) throw new Exception("Gói sao lưu vượt giới hạn cho phép");
                if ("manifest.json".equals(name)) {
                    JSONObject manifest = new JSONObject(new String(content, StandardCharsets.UTF_8));
                    if (!"personal-growth-backup".equals(manifest.optString("backupFormat")))
                        throw new Exception("Không đúng định dạng sao lưu Personal Growth");
                    if (manifest.optInt("backupVersion", 0) != 1)
                        throw new Exception("Phiên bản gói sao lưu chưa được hỗ trợ");
                    hasManifest = true;
                } else if ("data.json".equals(name)) {
                    data = new String(content, StandardCharsets.UTF_8);
                } else if ("report.xlsx".equals(name)) {
                    if (content.length < 4 || content[0] != 'P' || content[1] != 'K')
                        throw new Exception("Tệp Excel trong gói sao lưu không hợp lệ");
                    hasExcel = true;
                }
            }
        }
        if (!hasManifest) throw new Exception("Thiếu manifest.json");
        if (data == null) throw new Exception("Thiếu data.json");
        if (!hasExcel) throw new Exception("Thiếu report.xlsx");
        JSONObject root = new JSONObject(data);
        if (root.optJSONArray("cases") == null) throw new Exception("data.json không có danh sách hồ sơ");
        return root.toString();
    }

    private static String safeName(String raw) throws Exception {
        String name = raw == null ? "" : raw.replace('\\', '/');
        if (name.startsWith("/") || name.contains("../") || name.equals(".."))
            throw new Exception("Đường dẫn không an toàn trong ZIP");
        return name;
    }

    private static byte[] readEntry(ZipInputStream zip, long limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count; long total = 0;
        while ((count = zip.read(buffer)) > 0) {
            total += count;
            if (total > limit) throw new Exception("Tệp trong ZIP quá lớn");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(System.currentTimeMillis());
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] createExcel(JSONArray cases) throws Exception {
        List<Sheet> sheets = new ArrayList<>();
        Sheet overview = new Sheet("Overview", new String[]{"Chỉ số", "Giá trị"});
        overview.add("Ứng dụng", "Personal Growth");
        overview.add("Số hồ sơ", String.valueOf(cases.length()));
        overview.add("Nguồn khôi phục", "data.json");
        overview.add("Vai trò Excel", "Báo cáo và đối chiếu");
        sheets.add(overview);

        Sheet caseSheet = new Sheet("Cases", new String[]{"Case ID","Tiêu đề","Loại","Nhóm","Mức độ","Trạng thái","Bắt đầu","Cập nhật","Người liên quan","Địa điểm","Mô tả","Số cập nhật","Số phương án","Số ảnh"});
        Sheet updates = new Sheet("Updates", new String[]{"Case ID","STT","Loại cập nhật","Nội dung","Thời gian","Phút","Chi phí"});
        Sheet solutions = new Sheet("Solutions", new String[]{"Case ID","STT","Tên phương án","Mô tả","Ngày bắt đầu","Ngày đánh giá","Tiêu chí thành công"});
        Sheet finals = new Sheet("Final Results", new String[]{"Case ID","Kết quả thực tế","Điểm tốt","Điểm cần thay đổi","Bài học","Đánh giá","Hiệu quả","Quyết định","Ngày đóng"});
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.optJSONObject(i); if (c == null) continue;
            String id = c.optString("id", "");
            JSONArray us = c.optJSONArray("updates"); JSONArray ss = c.optJSONArray("solutions"); JSONArray ps = c.optJSONArray("photos");
            caseSheet.add(id,c.optString("title"),c.optString("type"),c.optString("category"),String.valueOf(c.optInt("severity",0)),c.optString("status"),c.optString("startedAt"),c.optString("updatedAt"),c.optString("owner"),c.optString("location"),c.optString("description"),String.valueOf(us==null?0:us.length()),String.valueOf(ss==null?0:ss.length()),String.valueOf(ps==null?0:ps.length()));
            if (us != null) for (int j=0;j<us.length();j++) { JSONObject u=us.optJSONObject(j); if(u!=null) updates.add(id,String.valueOf(j+1),u.optString("type"),first(u,"content","text","description"),first(u,"at","timestamp","createdAt","date"),String.valueOf(u.optInt("minutes",u.optInt("duration",0))),String.valueOf(u.optDouble("cost",0))); }
            if (ss != null) for (int j=0;j<ss.length();j++) { JSONObject so=ss.optJSONObject(j); if(so!=null) solutions.add(id,String.valueOf(j+1),first(so,"name","title"),first(so,"description","content"),first(so,"startDate","startedAt"),first(so,"reviewDate","evaluationDate"),first(so,"successCriteria","criteria")); }
            JSONObject f=c.optJSONObject("final"); if(f!=null) finals.add(id,first(f,"result","actualResult"),first(f,"good","strengths"),first(f,"bad","improvements"),first(f,"lesson","lessons"),first(f,"evaluation","assessment"),String.valueOf(f.optInt("effectiveness",0)),first(f,"decision","finalDecision"),c.optString("closedAt"));
        }
        sheets.add(caseSheet); sheets.add(updates); sheets.add(solutions); sheets.add(finals);
        return writeXlsx(sheets);
    }

    private static String first(JSONObject o, String... keys) { for(String k:keys){String v=o.optString(k,"");if(!v.isEmpty())return v;}return ""; }

    private static byte[] writeXlsx(List<Sheet> sheets) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            putText(zip,"[Content_Types].xml",contentTypes(sheets.size()));
            putText(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            putText(zip,"xl/workbook.xml",workbook(sheets));
            putText(zip,"xl/_rels/workbook.xml.rels",workbookRels(sheets.size()));
            putText(zip,"xl/styles.xml",styles());
            for(int i=0;i<sheets.size();i++) putText(zip,"xl/worksheets/sheet"+(i+1)+".xml",sheetXml(sheets.get(i)));
        }
        return bytes.toByteArray();
    }
    private static void putText(ZipOutputStream z,String n,String v)throws Exception{put(z,n,v.getBytes(StandardCharsets.UTF_8));}
    private static String contentTypes(int n){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");for(int i=1;i<=n;i++)b.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");return b.append("</Types>").toString();}
    private static String workbook(List<Sheet>s){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");for(int i=0;i<s.size();i++)b.append("<sheet name=\"").append(xml(s.get(i).name)).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");return b.append("</sheets></workbook>").toString();}
    private static String workbookRels(int n){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=1;i<=n;i++)b.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");b.append("<Relationship Id=\"rId").append(n+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");return b.toString();}
    private static String styles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"10\"/><name val=\"Calibri\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1477EA\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs></styleSheet>";}
    private static String sheetXml(Sheet s){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>");int cols=s.rows.isEmpty()?1:s.rows.get(0).length;for(int i=1;i<=cols;i++)b.append("<col min=\"").append(i).append("\" max=\"").append(i).append("\" width=\"").append(i==1?20:24).append("\" customWidth=\"1\"/>");b.append("</cols><sheetData>");for(int r=0;r<s.rows.size();r++){b.append("<row r=\"").append(r+1).append("\">");String[] row=s.rows.get(r);for(int c=0;c<row.length;c++){String ref=col(c+1)+(r+1);b.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");if(r==0)b.append(" s=\"1\"");b.append("><is><t xml:space=\"preserve\">").append(xml(row[c])).append("</t></is></c>");}b.append("</row>");}return b.append("</sheetData><autoFilter ref=\"A1:").append(col(cols)).append(Math.max(1,s.rows.size())).append("\"/></worksheet>").toString();}
    private static String col(int n){StringBuilder b=new StringBuilder();while(n>0){n--;b.insert(0,(char)('A'+n%26));n/=26;}return b.toString();}
    private static String xml(String s){if(s==null)return "";StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='&')b.append("&amp;");else if(c=='<')b.append("&lt;");else if(c=='>')b.append("&gt;");else if(c=='\"')b.append("&quot;");else if(c=='\'')b.append("&apos;");else if(c>=0x20||c=='\n'||c=='\r'||c=='\t')b.append(c);}return b.toString();}
    private static final class Sheet{final String name;final List<String[]> rows=new ArrayList<>();Sheet(String n,String[] h){name=n;rows.add(h);}void add(String...v){rows.add(v);}}
}
