package model;

import lombok.Getter;
import java.util.Objects;

public class TargetRange {

        @Getter
        String start;
        
        @Getter
        String end;

        @Getter
        Integer length;

        public TargetRange(Integer length, String start, String end) {
            Objects.requireNonNull(length, "length must not be null");
            Objects.requireNonNull(start,  "start must not be null");
            Objects.requireNonNull(end,    "end must not be null");
            this.length = length;
            this.start = start;
            this.end = end;
        }

    }
