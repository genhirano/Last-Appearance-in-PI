package model;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;

public class ProgressReportBean {

    @Getter
    @Setter
    private ZonedDateTime serverTime;// サーバーの現在時刻

    @Getter
    @Setter
    private Long allPiDataLength; // 検索対象のPIデータ全桁数

    @Getter
    @Setter
    private Map<File, Map<YCDFileUtil.FileInfo, String>> allFileInfo; // 検索対象のPIデータの全ヘッダー情報
 
    @Getter
    @Setter
    private Integer currentTargetLength; // 現在の検索対象の桁数

    @Getter
    @Setter
    private Integer currentDiscoveredCount;// 発見された数

    @Getter
    @Setter
    private Integer currentUndiscoveredCount; // 未発見の数

    @Getter
    @Setter
    private Float currentProgressRate; // 進捗率

    @Getter
    @Setter
    private String currentDeepestFind; // 現在までに発見されたもののうち、一番深いもの

    @Getter
    @Setter
    private Long currentDeepestFindPosition; // 現在までに発見されたものの一番深い位置

    @Getter
    @Setter
    private Long curenntElapsedTimeInSeconds; // 開始からの経過時間（秒）

    @Getter
    @Setter
    private List<Map<String, String>> result; // 過去の検索結果

    @Getter
    @Setter
    private TargetRange initSurvivalInfo; // カレントサバイバルリスト初期情報

    @Getter
    @Setter
    private Integer curenntSurvivalDiscoveredCount; // カレントユニット発見済数

    @Getter
    @Setter
    private Long curenntSurvivalDepth; // カレントユニットの現在探索深さ


    @Getter
    @Setter
    ZonedDateTime curenntSurvivalStartTime; // カレントサバイバルの開始時間

    @Getter
    @Setter
    Long currentSurvivalElapsedSeconds;// カレントサバイバルの処理時間(Sec)
 
    @Getter
    @Setter
    ArrayList<SurvivalList.DiscoverdInfo> discoverd;// 発見済み


    public ArrayList<Long> getDiscoverdPosList() {
        ArrayList<Long> discoverdPosList = new ArrayList<>();
        for (SurvivalList.DiscoverdInfo di : discoverd) {
            discoverdPosList.add(di.getFindPos());
        }
        
        Collections.sort(discoverdPosList);
        return discoverdPosList;
    }

    
    public ProgressReportBean() {
        this.result = new ArrayList<>();
    }

}
