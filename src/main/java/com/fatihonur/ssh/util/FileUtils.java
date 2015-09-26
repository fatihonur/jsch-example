
package com.fatihonur.ssh.util;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Fatih ONUR 2015
 *
 * Enjoy with the open source codes :) Help the community help the yourself...
 *******************************************************************************
 *----------------------------------------------------------------------------*/

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common file utilities.
 */
public class FileUtils {

    private final static Logger logger = LoggerFactory
            .getLogger(FileUtils.class);

    /**
     * Returns file paths of only sh and txt files at given
     * requested directory.
     *
     * @param dir
     *            the folder where files to be read
     * @return
     * @throws IOException
     *             if fails to read the file
     */
    public static List<Path> getSourceFiles(final Path dir) throws IOException {
        final List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{sh,txt}")) {
            for (final Path entry : stream) {
                result.add(entry);
                logger.debug("entry:{}", entry);
            }
        } catch (final DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an
            // IOException
            throw ex.getCause();
        }
        return result;
    }

}
