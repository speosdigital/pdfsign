package be.speos.pdf.sign.util;

import java.io.File;

public final class FileSystem {

    public static boolean exists(final String path) {
        try {
            return (new File(new File(path).getCanonicalPath()).exists());
        } catch (Exception error) {
            return false;
        }
    }

    public static boolean isRoot(final String path) {
        try {
            boolean pathIsRoot = false;

            File roots[] = File.listRoots();

            String pathToCheck = path;
            if (!path.endsWith(File.separator)) {
                pathToCheck = path + File.separator;
            }

            for (File root : roots) {
                if (pathToCheck.toUpperCase().equals(root.getPath().toUpperCase())) {
                    pathIsRoot = true;
                    break;
                }
            }

            return pathIsRoot;
        } catch (Exception error) {
            return false;
        }
    }

    public static String getDirPath(final String path) {
        try {
            String directoryPath;

            if (FileSystem.isRoot(path)) {
                if (!path.endsWith(File.separator)) {
                    directoryPath = path + File.separator;
                } else {
                    directoryPath = path;
                }
            } else {
                File fsObj = new File(new File(path).getCanonicalPath());

                if (fsObj.exists()) {
                    directoryPath = fsObj.getParent();
                } else {
                    StringBuffer parentPath = new StringBuffer(fsObj.getCanonicalPath());
                    if (parentPath.indexOf(File.separator) > 0) {
                        directoryPath = parentPath.substring(0, parentPath.lastIndexOf(File.separator));
                    } else {
                        directoryPath = fsObj.getCanonicalPath();
                    }
                }
            }

            return directoryPath;
        } catch (Exception error) {
            return null;
        }
    }

    public static boolean createDir(final String dirPath) {
        try {
            return new File(new File(dirPath).getCanonicalPath()).mkdirs();
        } catch (Exception error) {
            return false;
        }
    }

}
