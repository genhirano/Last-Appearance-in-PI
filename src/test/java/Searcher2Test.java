import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;


import java.util.List;

import org.junit.jupiter.api.Test;

import controller.Searcher2;
import model.TargetRange;

public class Searcher2Test {

    @Test
    public void testDivideTargetRange10() throws Exception {

        List<TargetRange> result = Searcher2.divideTargetRange(10, new TargetRange(10, "0", "99"));

        // 結果の検証
        assertEquals(10, result.size());
        assertEquals("0", result.get(0).getStart());
        assertEquals("9", result.get(0).getEnd());
        
        assertEquals("10", result.get(1).getStart());
        assertEquals("19", result.get(1).getEnd());

        assertEquals("20", result.get(2).getStart());
        assertEquals("29", result.get(2).getEnd());
        
        assertEquals("90", result.get(9).getStart());
        assertEquals("99", result.get(9).getEnd());

        //範囲外アクセステスト(例外が発生すること)
        try {
            result.get(10);
        } catch (IndexOutOfBoundsException e) {
            assertEquals(IndexOutOfBoundsException.class, e.getClass());
        }

    }

    @Test
    public void testDivideTargetRange7() throws Exception {

        List<TargetRange> result = Searcher2.divideTargetRange(7, new TargetRange(10, "0", "99"));

        // 結果の検証
        assertEquals(7, result.size());
        assertEquals("0", result.get(0).getStart());
        assertEquals("13", result.get(0).getEnd());
        
        assertEquals("14", result.get(1).getStart());
        assertEquals("27", result.get(1).getEnd());

        assertEquals("84", result.get(6).getStart());
        assertEquals("99", result.get(6).getEnd());
        
        //範囲外アクセステスト(例外が発生すること)
        try {
            result.get(7);
        } catch (IndexOutOfBoundsException e) {
            assertEquals(IndexOutOfBoundsException.class, e.getClass());
        }

    }


}
