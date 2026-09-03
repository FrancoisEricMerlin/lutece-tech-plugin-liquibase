package fr.paris.lutece.plugins.liquibase.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import fr.paris.lutece.portal.service.util.AppLogService;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.Resource;

/**
 * Registry of the SQL scripts carrying the runFirst directive in their leading comment block (LUT-33299) :
 *
 * <pre>
 * --liquibase formatted sql
 * --lutece runFirst
 * --changeset author:id
 * </pre>
 *
 * Such a script is executed in a preliminary liquibase update, BEFORE the main changelog is built and
 * filtered. It is an ordinary liquibase formatted SQL file (changesets recorded in DATABASECHANGELOG, run
 * once, preconditions, checksums), so a plugin can ship one-shot fix-ups that must be visible to the version
 * resolution of the main run : typically, after a plugin rename, migrating the datastore keys
 * (core.plugins.status.&lt;old&gt;.* to &lt;new&gt;.*, instance-prefixed forms included) and rewriting the
 * DATABASECHANGELOG FILENAME rows to the new component directory.
 *
 * Authoring rules for such scripts : they run on every database state, including an empty one at first
 * install, so every changeset must be guarded by preconditions failing to MARK_RAN both onFail and onError
 * (a referenced table may not exist yet) ; and, like any executed changeset, their body must not change
 * afterwards without a validCheckSum declaration.
 *
 * The scan runs once, lazily, in the single startup thread (deliberately not thread-safe, like the rest of
 * this plugin).
 */
public final class RunFirstScripts
{
    private static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    private static final Pattern RUN_FIRST_PATTERN = Pattern.compile("^--\\s*lutece\\b.*\\brunFirst\\b");
    private static final int MAX_HEADER_LINES = 20;

    /** normalized paths of the runFirst scripts. Built on first use. */
    private static Set<String> paths;

    private RunFirstScripts( )
    {
    }

    /** Whether at least one script declares the runFirst directive. */
    public static boolean any()
    {
        return !scan().isEmpty();
    }

    /** Whether the given changelog path (with or without WEB-INF/classes/) declares runFirst. */
    public static boolean isRunFirst(String path)
    {
        return scan().contains(normalize(path));
    }

    private static Set<String> scan()
    {
        if (paths == null)
        {
            paths = new HashSet<>();
            try (ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor())
            {
                for (Resource resource : accessor.search("sql", true))
                {
                    String path = normalize(resource.getPath());
                    if (path.endsWith(".sql") && !paths.contains(path) && declaresRunFirst(resource, path))
                    {
                        AppLogService.info("LiquibaseRunner. Script {} declares runFirst : it will run before the main changelog", path);
                        paths.add(path);
                    }
                }
            } catch (Exception e)
            {
                AppLogService.error("LiquibaseRunner. Could not scan SQL files for runFirst directives : directives ignored", e);
            }
        }
        return paths;
    }

    private static boolean declaresRunFirst(Resource resource, String path)
    {
        try (InputStream stream = resource.openInputStream())
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count++ < MAX_HEADER_LINES)
            {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--"))
                {
                    // past the leading comment block : the directive must appear before any SQL
                    return false;
                }
                if (RUN_FIRST_PATTERN.matcher(trimmed).find())
                {
                    return true;
                }
            }
        } catch (IOException e)
        {
            AppLogService.error("LiquibaseRunner. Could not read header of " + path, e);
        }
        return false;
    }

    private static String normalize(String path)
    {
        return path.replace(WEB_INF_CLASSES, "");
    }
}
