package com.alexkasko.maven.jniheaders;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.io.IOUtils.copy;

/**
 * Runs {@code javah} utility on provided class and writes generated header to specified location.
 *
 * @goal javah
 *
 * @author alexkasko
 * Date: 4/11/13
 */
public class JavahMojo extends AbstractMojo {

    /**
     * Path to {@code javah} executable
     *
     * @parameter expression="${javahPath}"
     */
    private File javahPath;

    /**
     * {@code -verbose} parameter to "javah"
     *
     * @parameter expression="${javahVerbose}" default-value="false"
     */
    private boolean javahVerbose;
    /**
     *
     * Class to run {@code javah} on
     *
     * @required
     * @parameter expression="${javahclass}"
     */
    private String javahClass;
    /**
     * Path to output header file
     *
     * @required
     * @parameter expression="${javahOutputFilePath}"
     */
    private File javahOutputFilePath;
    /**
     * Project {@code src/main} directory
     *
     * @parameter expression="${project.build.sourceDirectory}"
     * @readonly
     */
    private File srcMainDirectory;
    /**
     * Project output directory
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @readonly
     */
    private File classesDirectory;
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File srcFile = new File(srcMainDirectory, javahClass.replace(".","/") + ".java");
            if(srcFile.exists() && srcFile.isFile() && javahOutputFilePath.exists() && javahOutputFilePath.isFile() &&
                    srcFile.lastModified() <= javahOutputFilePath.lastModified()) {
                getLog().info("Source file: [" + srcFile.getAbsolutePath() + "] is not modified, skipping 'javah' execution");
                return;
            }
            long now = currentTimeMillis();
            File javah = null != javahPath ? javahPath : new File(Utils.jdkHome(getLog()), "bin/javah");
            if(!(javah.exists() && javah.isFile())) throw new IOException("Cannot find javah path, check 'javahPath' property");
            List<String> command = new ArrayList<String>();
            command.add(javah.getAbsolutePath());
            if(javahVerbose) command.add("-verbose");
            command.add("-o");
            command.add(javahOutputFilePath.getAbsolutePath());
            Set<Artifact> ars = project.getArtifacts();
            if(ars.size() > 0) {
                command.add("-classpath");
                for(Artifact ar : ars) command.add(ar.getFile().getAbsolutePath());
            }
            command.add(javahClass);
            getLog().info(command.toString());
            // start process
            Process proc = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .directory(classesDirectory)
                    .start();
            copy(proc.getInputStream(), System.out);
            proc.waitFor();
            // touch file in case javah skip it's updating
            if(0 == proc.exitValue() && javahOutputFilePath.exists() && javahOutputFilePath.isFile() &&
                    javahOutputFilePath.lastModified() < now) javahOutputFilePath.setLastModified(currentTimeMillis());
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
