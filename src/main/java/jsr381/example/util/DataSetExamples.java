package jsr381.example.util;

import deepnetts.data.BasicDataSet;
import deepnetts.data.BasicDataSetItem;
import deepnetts.util.DeepNettsException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Kevin Berendsen
 */
public class DataSetExamples {

    public static BasicDataSet getSonarDataSet() throws IOException {
        return DataSetExamples.fromURL(new URL("https://raw.githubusercontent.com/JavaVisRec/jsr381-examples-datasets/master/sonar.csv"),
                ",", 60, 1, false);
    }

    public static BasicDataSet getIrisClassificationDataSet() throws IOException {
        return DataSetExamples.fromURL(new URL("https://raw.githubusercontent.com/JavaVisRec/jsr381-examples-datasets/master/iris_data_normalised.txt"),
                ",", 4, 3, true);
    }

    public static BasicDataSet getSwedishAutoInsuranceDataSet() throws IOException {
        return DataSetExamples.fromURL(new URL("https://raw.githubusercontent.com/JavaVisRec/jsr381-examples-datasets/master/SwedenAutoInsurance.csv"),
                ",", 1, 1, false);
    }

    private static BasicDataSet fromURL(URL url, String delimiter, int inputsNum, int outputsNum, boolean hasColumnNames) throws IOException {
        BasicDataSet dataSet = new BasicDataSet(inputsNum, outputsNum);

        URLConnection conn = url.openConnection();
        String[] content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().toArray(String[]::new);
        }
        if (content == null) {
            throw new NullPointerException("content == null");
        } else if (content.length <= 1 && hasColumnNames) {
            throw new IllegalArgumentException("content has one line of columns");
        } else if (content.length == 0) {
            throw new IllegalArgumentException("content has no lines");
        }

        int skipCount = 0;
        if (hasColumnNames) {    // get col names from the first line
            String[] colNames = content[0].split(delimiter);
            dataSet.setColumnNames(colNames);
            skipCount = 1;
        } else {
            String[] colNames = new String[inputsNum+outputsNum];
            for(int i=0; i<inputsNum;i++)
                colNames[i] = "in"+(i+1);

            for(int j=0; j<outputsNum;j++)
                colNames[inputsNum+j] = "out"+(j+1);

            dataSet.setColumnNames(colNames);
        }


        Arrays.stream(content)
                .skip(skipCount)
                .filter(l -> !l.isEmpty())
                .map(l -> toBasicDataSetItem(l, delimiter, inputsNum, outputsNum))
                .forEach(dataSet::add);
        return dataSet;
    }

    public static File getMnistTestingDataSet() throws IOException {
        File folder = Paths.get(System.getProperty("java.io.tmpdir"), "visrec-datasets", "mnist", "testing").toFile();

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new IOException("Couldn't create temporary directories to download the Mnist testing dataset.");
            }
        }

        downloadZip("https://github.com/JavaVisRec/jsr381-examples-datasets/raw/master/mnist_testing_data_png.zip", folder);

        return folder;
    }

    public static File getMnistTrainingDataSet() throws IOException {
        File folder = Paths.get(System.getProperty("java.io.tmpdir"), "visrec-datasets", "mnist", "training").toFile();

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new IOException("Couldn't create temporary directories to download the Mnist training dataset.");
            }
        }

        downloadZip("https://github.com/JavaVisRec/jsr381-examples-datasets/raw/master/mnist_training_data_png.zip", folder);

        return folder;
    }

    private static BasicDataSetItem toBasicDataSetItem(String line, String delimiter, int inputsNum, int outputsNum) {
        String[] values = line.split(delimiter);
        if (values.length != (inputsNum + outputsNum)) {
            throw new DeepNettsException("Wrong number of values found " + values.length + " expected " + (inputsNum + outputsNum));
        }
        float[] in = new float[inputsNum];
        float[] out = new float[outputsNum];

        try {
            // these methods could be extracted into parse float vectors
            for (int i = 0; i < inputsNum; i++) { //parse inputs
                in[i] = Float.parseFloat(values[i]);
            }

            for (int j = 0; j < outputsNum; j++) { // parse outputs
                out[j] = Float.parseFloat(values[inputsNum + j]);
            }
        } catch (NumberFormatException nex) {
            throw new DeepNettsException("Error parsing csv, number expected: " + nex.getMessage(), nex);
        }

        return new BasicDataSetItem(in, out);
    }

    private static void downloadZip(String httpsURL, File basePath)  {
        URL url = null;
        try {
            File toFile = new File(basePath, "temp.zip");
            url = new URL(httpsURL);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(toFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
            unzip(toFile);
            if (!toFile.delete()) {
                toFile.deleteOnExit();
            }
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private static void unzip(File fileToBeUnzipped) {
        File dir = new File(fileToBeUnzipped.getParent());

        if(!dir.exists())
            dir.mkdirs();

        try {
            ZipFile zipFile = new ZipFile(fileToBeUnzipped.getAbsoluteFile());
            Enumeration<?> enu = zipFile.entries();
            while (enu.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enu.nextElement();
                String name = zipEntry.getName();

                if (name.contains(".DS_Store") || name.contains("__MACOSX"))
                    continue;

                File file = Paths.get(fileToBeUnzipped.getParent(), name).toFile();
                if (name.endsWith("/")) {
                    file.mkdirs();
                    continue;
                }

                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                InputStream is = zipFile.getInputStream(zipEntry);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = is.read(bytes)) >= 0) {
                    fos.write(bytes, 0, length);
                }
                is.close();
                fos.close();
            }
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
