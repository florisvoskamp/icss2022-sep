package nl.han.ica.icss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionFeatureTest {

    @Test
    void minOfTwoPixelsYieldsSmallerInCss() {
        Pipeline p = new Pipeline();
        p.parseString("p { width: min(30px, 10px); }");
        assertTrue(p.check());
        p.transform();
        String css = p.generate();
        assertTrue(css.contains("10px"), css);
    }

    @Test
    void maxOfTwoPixelsYieldsLargerInCss() {
        Pipeline p = new Pipeline();
        p.parseString("p { width: max(30px, 10px); }");
        assertTrue(p.check());
        p.transform();
        String css = p.generate();
        assertTrue(css.contains("30px"), css);
    }

    @Test
    void checkerRejectsMixedMinMaxOperands() {
        Pipeline p = new Pipeline();
        p.parseString("p { width: min(10px, 5%); }");
        assertFalse(p.check());
    }

    @Test
    void checkerRejectsVariableTypeChange() {
        Pipeline p = new Pipeline();
        p.parseString("A := 10px;\nA := 5%;\np { width: 1px; }");
        assertFalse(p.check());
    }

    @Test
    void minWithPercentagesInCss() {
        Pipeline p = new Pipeline();
        p.parseString("p { width: min(50%, 80%); }");
        assertTrue(p.check());
        p.transform();
        String css = p.generate();
        assertTrue(css.contains("50%"), css);
    }
}
