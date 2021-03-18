package gov.dhs.cisa.flare.misp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MispTransHelper {
	private Logger logger = LoggerFactory.getLogger(MispTransHelper.class);

	private HashMap<String, Tracker> trackers = new HashMap<String, Tracker>();
	private Schema schema = new Schema.Parser().parse(new File("config/tracker.avsc"));
	private DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
	private DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);

	public MispTransHelper() throws IOException {
		logger.info("Constructing .........................");
		
		String tracking_file = Config.getProperty("mtc.tracking.datafile");

		File file = new File(tracking_file);
		if (!file.exists()) {
			logger.info("Creating a new Avro file.....{}", file.getAbsolutePath());
			dataFileWriter.create(schema, file);
		} else {
			DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
			DataFileReader<GenericRecord> dataFileReader = new DataFileReader<GenericRecord>(file, datumReader);
			dataFileReader.forEach(rec -> {
				Tracker tracker = deserializeTracker(rec);
				logger.debug("tracker: stix_id :{}", tracker.getStixId());
				trackers.put(tracker.getStixId(), tracker);
			});
			dataFileReader.close();
			dataFileWriter.appendTo(file);
		}
	}

	public Tracker deserializeTracker(GenericRecord rec) {
		Tracker tracker = new Tracker();
		tracker.setStixId(String.valueOf(rec.get("stix_id")));
		tracker.setEventId(String.valueOf(rec.get("event_id")));
		return tracker;
	}

	public GenericRecord serializeUser(Tracker tracker) throws IOException {
		logger.debug("storing tracker: {}", tracker.getStixId());
		GenericRecord rec = new GenericData.Record(schema);
		rec.put("stix_id", tracker.getStixId());
		rec.put("event_id", tracker.getEventId());
		dataFileWriter.append(rec);
		dataFileWriter.flush();
		trackers.put(tracker.getStixId(), tracker);
		return rec;
	}

	public Boolean exists(Tracker tracker) {
		if (trackers.get(tracker.stixId) == null) {
			return false;
		}
		return true;
	}
}
