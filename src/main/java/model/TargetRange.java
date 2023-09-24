package model;

import lombok.Getter;

public class TargetRange {

        @Getter
        String start;
        
        @Getter
        String end;

        @Getter
        Integer length;

        public TargetRange(Integer length, String start, String end) {
            this.length = length;
            this.start = start;
            this.end = end;
        }

    }
