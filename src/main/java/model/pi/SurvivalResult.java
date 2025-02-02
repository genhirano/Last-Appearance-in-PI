package model.pi;

import lombok.Getter;
import lombok.Setter;

/**
     * サバイバル結果を保持するクラス(メソッド戻り値として使用)
     */
    public class SurvivalResult implements Comparable<SurvivalResult> {

        @Getter
        @Setter
        private String target;

        @Getter
        @Setter
        private Long findPos;

        public SurvivalResult(String target, Long findPos) {
            this.target = target;
            this.findPos = findPos;
        }

        public void update(SurvivalResult other) {
            this.target = other.target;
            this.findPos = other.findPos;
        }

        /**
         * オブジェクト比較.
         * 
         * @param other
         * @return 自分のほうが小さい場合はマイナス、大きい場合はプラス、同じ場合はゼロ
         */
        @Override
        public int compareTo(SurvivalResult other) {
            return this.findPos.compareTo(other.findPos);
        }

    }

