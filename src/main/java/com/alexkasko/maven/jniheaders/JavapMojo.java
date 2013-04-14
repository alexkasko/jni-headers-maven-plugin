package com.alexkasko.maven.jniheaders;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.CaseFormat.*;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

/**
 * Runs {@code javap} on provided class and writes generated output
 * formatted as C/C++ header file containing macros definitions for
 * methods names and signatures
 *
 * @goal javap
 * @author alexkasko
 * Date: 4/11/13
 */
public class JavapMojo extends AbstractMojo {
    /**
     * Regular expression to match {@code javap} error messages
     *
     * @parameter expression="${javapParseErrorRegex}" default-value="^ERROR:.*$"
     */
    private String javapParseErrorRegexString;
    /**
     * Regular expression to match {@code javap} method names
     *
     * @parameter expression="${javapParseNameRegex}" default-value="^.*\\s+([^.\\s]+)\\(.*\\);$"
     */
    private String javapParseNameRegexString;
    /**
     * Regular expression to match {@code javap} method signatures
     *
     * @parameter expression="${javapParseSignatureRegex}" default-value="^\\s+Signature:\\s+(.+)$"
     */
    private String javapParseSignatureRegexString;
    /**
     * Path to "javap" executable
     *
     * @parameter expression="${javapPath}"
     */
    private File javapPath;
    /**
     * Class to run {@code javap} on
     *
     * @required
     * @parameter expression="${javapClass}"
     */
    private String javapClass;
    /**
     * Path to outputFile
     *
     * @required
     * @parameter expression="${javapOutputFile}"
     */
    private File javapOutputFilePath;
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
     * The maven project
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
            File srcFile = new File(srcMainDirectory, javapClass.replace(".","/") + ".java");
            if(srcFile.exists() && srcFile.isFile() && javapOutputFilePath.exists() && javapOutputFilePath.isFile() &&
                    srcFile.lastModified() <= javapOutputFilePath.lastModified()) {
                getLog().info("Source file: [" + srcFile.getAbsolutePath() + "] is not modified, skipping 'javap' execution");
                return;
            }
            File javah = null != javapPath ? javapPath : new File(Utils.jdkHome(), "bin/javap");
            if (!(javah.exists() && javah.isFile())) throw new IOException("Cannot find javap path, check 'javapPath' property");
            List<String> command = new ArrayList<String>();
            command.add(javah.getAbsolutePath());
            command.add("-s");
            Set<Artifact> ars = project.getArtifacts();
            if (ars.size() > 0) {
                command.add("-classpath");
                for (Artifact ar : ars) command.add(ar.getFile().getAbsolutePath());
            }
            command.add(javapClass);
            getLog().info(command.toString());
            // start process
            Process proc = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .directory(classesDirectory)
                    .start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copy(proc.getInputStream(), baos);
            proc.waitFor();
            String out = new String(baos.toByteArray(), Charset.forName("UTF-8"));
            int exit = proc.exitValue();
            if(exit > 0) throw new IOException("javap exited with code: [" + exit + "], output: [" + out + "]");
            writeHeader(out);
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void writeHeader(String out) throws IOException {
        Pattern errorRegex = Pattern.compile(javapParseErrorRegexString);
        Pattern nameRegex = Pattern.compile(javapParseNameRegexString);
        Pattern signatureRegex = Pattern.compile(javapParseSignatureRegexString);
        BufferedReader reader = new BufferedReader(new StringReader(out));
        Writer writer = null;
        try {
            String fullClassName = javapClass.replace(".","_");
            writer = new BufferedWriter(new OutputStreamWriter(openOutputStream(javapOutputFilePath)));
            writer.append("/* DO NOT EDIT THIS FILE - it is machine generated */").append("\n");
            writer.append("/* Header for class ").append(fullClassName).append(" */").append("\n");
            writer.append("\n");
            writer.append("#ifndef _Callbacks_").append(fullClassName).append("\n");
            writer.append("#define _Callbacks_").append(fullClassName).append("\n");
            writer.append("\n");

            String upper = null;
            String line;
            while (null != (line = reader.readLine())) {
                if(errorRegex.matcher(line).matches()) throw new IOException(line);
                Matcher nameMatcher = nameRegex.matcher(line);
                if(nameMatcher.matches()) {
                    String name = nameMatcher.group(1);
                    upper = LOWER_CAMEL.to(UPPER_UNDERSCORE, name);
                    writer.append("/* ").append(line).append(" */").append("\n");
                    writer.append("#define ").append(upper).append("_NAME").append(" ")
                            .append("\"").append(name).append("\"").append("\n");
                }
                Matcher sigMatcher = signatureRegex.matcher(line);
                if(sigMatcher.matches()) {
                    if(null == upper) throw new IOException("Cannot parse signature - no name parsed, line: [" + line + "]");
                    String sig = sigMatcher.group(1);
                    writer.append("/* ").append(line).append(" */").append("\n");
                    writer.append("#define ").append(upper).append("_SIGNATURE").append(" ")
                            .append("\"").append(sig).append("\"").append("\n");
                    writer.append("\n");
                    upper = null;
                }
            }
            writer.append("#endif //_Callbacks_").append(fullClassName).append("\n");
        } finally {
            closeQuietly(writer);
        }
    }
}
