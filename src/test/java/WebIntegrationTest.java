import controller.Env;
import controller.Main;
import controller.Searcher;
import controller.StoreController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 結合テスト：Main を同一 JVM 内で起動し、テスト用円周率データ（1,500万桁）で検索を走らせながら WEB 画面を取得する.
 *
 * 1. 起動直後（進行中の桁がまだ無い瞬間を含む）でも HTTP 200 で描画できる
 * 2. 検索が進んだ後も HTTP 200 で、画面に桁数・発見数・出現分布が含まれる
 * 3. 保存ファイル（NNN.txt）が桁ごとに作られ、1〜5桁の最遅出現が OEIS と一致する
 * 4. データが尽きたとき（7桁は 1,500万桁では終わらない）にプロセスを落とさず、検索スレッドだけ停止して WEB は応答し続ける
 * 5. 静的 HTML（index.html）が保存される
 */
public class WebIntegrationTest {

    private static final int PORT = 18090;

    @TempDir
    static Path tempDir;

    /** プロジェクト直下の index.html（Web が上書きするため、テスト後に元へ戻す） */
    private static final Path INDEX_HTML = Paths.get("index.html");
    private static byte[] indexHtmlBackup;

    @AfterAll
    static void クリーンアップ() throws Exception {
        Spark.stop();
        Spark.awaitStop();
        if (indexHtmlBackup != null) {
            Files.write(INDEX_HTML, indexHtmlBackup);
        }
        Field f = Env.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
        Env.setPropFileName(null);
    }

    @Test
    void 起動から検索停止までWEB画面が応答し続け結果が正しい() throws Exception {

        if (Files.exists(INDEX_HTML)) {
            indexHtmlBackup = Files.readAllBytes(INDEX_HTML);
        }

        // プロパティファイル作成
        URL url = WebIntegrationTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        Path out = tempDir.resolve("out");
        Files.createDirectories(out);
        StringBuilder prop = new StringBuilder();
        prop.append("outputPath=").append(out.toString().replace("\\", "/")).append("\n");
        prop.append("port=").append(PORT).append("\n");
        prop.append("listSize=100000\n");
        prop.append("unitLength=380000\n");
        for (int i = 0; i < 15; i++) {
            prop.append(String.format("ycd%03d=%s/Pi - Dec - Chudnovsky - %d.ycd%n", i,
                    dir.getPath().replace("\\", "/"), i));
        }
        Path propFile = tempDir.resolve("it.properties");
        Files.write(propFile, prop.toString().getBytes(StandardCharsets.UTF_8));

        Field f = Env.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);

        // 起動（検索スレッド開始 ＋ WEB サーバー起動）
        Main.main(new String[]{propFile.toString()});
        Spark.awaitInitialization();

        // 1. 起動直後から WEB が応答する（10 回連続で取得し、すべて 200 であること。進行中の桁が切り替わる瞬間を含む）
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codes.add(get("/").code);
            Thread.sleep(200);
        }
        for (int i = 0; i < codes.size(); i++) {
            assertEquals(200, codes.get(i), "取得 " + i + " 回目の HTTP ステータス（500 の場合は標準出力の 'Web page rendering error' を参照）");
        }

        // 3./4. データが尽きて検索スレッドが停止するまで待つ（1〜6桁は完了し、7桁で尽きる）
        long deadline = System.currentTimeMillis() + 120_000L;
        while (Searcher.getStoppedReason() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
        }
        assertNotNull(Searcher.getStoppedReason(), "テストデータでは 7 桁でデータが尽きて検索が停止すること");

        // 2. 停止後も WEB が応答し、画面に主要項目が含まれる
        Response res = get("/");
        assertEquals(200, res.code);
        assertTrue(res.body.contains("Discoverd"), "発見数の項目があること");
        assertTrue(res.body.contains("Appearance Distribution"), "出現分布の項目があること");
        assertTrue(res.body.contains("検索停止:"), "停止理由が表示されること");
        assertEquals(7, StoreController.getProgressReport().getCurrentTargetLength(), "保存ファイルがない最初のサイクルの桁数");
        assertTrue(res.body.contains("0000000"), "停止した探索範囲が表示されること");
        assertTrue(res.body.contains("1,369,560"), "5桁の最遅出現位置 1,369,560 が結果表に表示されること");
        assertTrue(res.body.contains("14,118,307"), "6桁の最遅出現位置 14,118,307 が結果表に表示されること");

        // 3. 保存ファイルの内容が OEIS と一致する
        assertEquals("0,9,0,32", firstFields(out.resolve("001.txt"), 4));
        assertEquals("00,99,68,605", firstFields(out.resolve("002.txt"), 4));
        assertEquals("000,999,483,8553", firstFields(out.resolve("003.txt"), 4));
        assertEquals("0000,9999,6716,99846", firstFields(out.resolve("004.txt"), 4));
        assertEquals("00000,99999,33394,1369560", firstFields(out.resolve("005.txt"), 4));
        List<String> lines6 = Files.readAllLines(out.resolve("006.txt"));
        assertEquals(10, lines6.size(), "6桁は 100,000 件ずつ 10 範囲");
        assertTrue(lines6.get(5).startsWith("500000,599999,569540,14118307,"), "6桁の最遅出現は 569540 @ 14,118,307");
        assertFalse(Files.exists(out.resolve("007.txt")), "7桁は完了しないので保存されないこと");

        // 5. 静的 HTML が保存される
        assertTrue(Files.exists(INDEX_HTML));
        assertTrue(Files.size(INDEX_HTML) > 1000);
    }

    private static String firstFields(Path file, int n) throws IOException {
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size(), file.getFileName() + " は 1 行であること");
        String[] s = lines.get(0).split(",");
        return String.join(",", java.util.Arrays.copyOf(s, n));
    }

    private static class Response {
        int code;
        String body;
    }

    private static Response get(String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:" + PORT + path).openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(30000);
        Response r = new Response();
        r.code = con.getResponseCode();
        InputStream in = (r.code < 400) ? con.getInputStream() : con.getErrorStream();
        r.body = (in == null) ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        con.disconnect();
        return r;
    }
}
