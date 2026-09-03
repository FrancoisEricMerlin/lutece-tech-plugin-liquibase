package fr.paris.lutece.plugins.liquibase.filters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validates the detection of the runFirst directive from the SQL fixtures under
 * src/test/resources/sql/plugins : ppp ships a fix-up script declaring it, the other fixtures do not.
 */
public class RunFirstScriptsTest
{
    private static final String PPP_FIXUP = "sql/plugins/ppp/core/init_core_ppp_fixup.sql";
    private static final String MMM_CREATE = "sql/plugins/mmm/plugin/create_db_mmm.sql";

    @Test
    public void directiveIsDetected()
    {
        assertTrue(RunFirstScripts.any(), "at least one fixture declares runFirst");
        assertTrue(RunFirstScripts.isRunFirst(PPP_FIXUP), PPP_FIXUP + " declares runFirst");
        assertTrue(RunFirstScripts.isRunFirst("WEB-INF/classes/" + PPP_FIXUP), "paths are normalized");
    }

    @Test
    public void otherScriptsAreNotSelected()
    {
        assertFalse(RunFirstScripts.isRunFirst(MMM_CREATE), MMM_CREATE + " does not declare runFirst");
        assertFalse(new RunFirstIncludeAllFilter().include(MMM_CREATE));
        assertTrue(new RunFirstIncludeAllFilter().include(PPP_FIXUP));
    }
}
