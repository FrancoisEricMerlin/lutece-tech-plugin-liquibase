package fr.paris.lutece.plugins.liquibase.filters;

import liquibase.changelog.IncludeAllFilter;

/**
 * Filter of the preliminary changelog (db/changelog-pre.xml) : selects ONLY the scripts declaring the
 * runFirst directive, executed before the main changelog is built and filtered (LUT-33299). The main
 * changelog filter ({@link TestIncludeAllFilter}) excludes them symmetrically.
 *
 * Instantiated by liquibase itself (no-arg constructor), outside any DI.
 */
public class RunFirstIncludeAllFilter implements IncludeAllFilter
{
    @Override
    public boolean include(String changeLogPath)
    {
        return changeLogPath.endsWith(".sql") && RunFirstScripts.isRunFirst(changeLogPath);
    }
}
