package bme.iclef.hadoop.seq;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.SequenceFileInputFormat;

public class ImageInputFormat extends SequenceFileInputFormat<Text, BytesWritable> {

}
