package model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;


public class ProgressReportBean {

    @Getter @Setter
    private Long serverTime;//サーバーの現在時刻

    @Getter @Setter
    private Long serverCUPSpec;//サーバーのCPUスペック

    @Getter @Setter
    private Long allPiDataLength; //検索対象のPIデータ全桁数
    
    @Getter @Setter
    private Integer currentTargetLength; //現在の検索対象の桁数
    
    @Getter @Setter
    private Long currentDiscoveredCount;//発見された数
    
    @Getter @Setter
    private Long currentUndiscoveredCount;  //未発見の数
    
    @Getter @Setter
    private Long currentDeepestFindPosition;   //現在までに発見されたものの一番深い位置
    
    @Getter @Setter
    private Long curenntElapsedTimeInSeconds; //開始からの経過時間（秒）

    @Getter @Setter
    private List<String> result; //過去の検索結果
   
 


}
