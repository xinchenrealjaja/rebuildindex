package rebuildindex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import listing.models.IndexType;
import models.CommListingBrief;
import models.ListingBrief;
import models.ResiListingBrief;
import play.core.server.noCATrustManager;
import play.libs.Json;
import utils.FileUtils;

public class rebuildindex {

	static StringBuilder listingName = new StringBuilder();

	public List<ResiListingBrief> getAllResiListingBriefs(IndexType indexType) {
		try {
			String fileName = (indexType == IndexType.Available)
					? "data/" + listingName.toString() + "/listings/index.txt"
					: "data/" + listingName.toString() + "/delistings/30days.txt";
			if (new File(fileName).exists() == false) {
				System.out.println("didn't find the index.txt, please confirm the path");
				return new ArrayList<>();
			}
			// assuming adapter own index file
			String listingTxt = getFileContent(fileName);

			JsonNode jnode = Json.parse(listingTxt);
			ResiListingBrief[] listings = Json.fromJson(jnode, ResiListingBrief[].class);

			List<ResiListingBrief> briefs = new ArrayList<>();
			for (ResiListingBrief item : listings) {
				briefs.add(item);
			}
			return briefs;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static Map<String, String> indexCache = new HashMap<>();

	static String getFileContent(String fileName) throws Exception {
		// assuming adapter own index file
		String listingTxt;
		synchronized (indexCache) {
			if (indexCache.containsKey(fileName)) {
				listingTxt = indexCache.get(fileName);
			} else
				listingTxt = FileUtils.getFileContent(fileName);
		}
		return listingTxt;
	}

	public List<CommListingBrief> getAllCommListingBriefs(IndexType indexType) {
		try {
			String fileName = (indexType == IndexType.Available)
					? "data/" + listingName.toString() + "/listings/indexComm.txt"
					: "data/" + listingName.toString() + "/delistings/30daysComm.txt";
			if (new File(fileName).exists() == false)
				return new ArrayList<>();

			// assuming adapter own index file
			String listingTxt = getFileContent(fileName);

			JsonNode jnode = Json.parse(listingTxt);
			CommListingBrief[] listings = Json.fromJson(jnode, CommListingBrief[].class);

			List<CommListingBrief> briefs = new ArrayList<>();
			for (CommListingBrief item : listings) {
				briefs.add(item);
			}
			return briefs;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <T extends ListingBrief> Map<String, T> toMap(List<T> array) {
		Map<String, T> ret = new HashMap<>();
		for (T item : array) {
			ret.put(item.propertyId, item);
		}
		return ret;
	}

	public static Map<String, String> arrayToMap(String[] parameters) {
		Map<String, String> map = new HashMap<>();
		for (int i = 1; i < parameters.length; i++) {
			String[] strings = parameters[i].split("=");
			map.put(strings[0], strings[1]);
		}
		return map;
	}

	public void saveCommIndex(Collection<CommListingBrief> briefs, IndexType indexType) {
		String fileName = "data/" + listingName.toString() + "/listings/indexComm.txt";
		if (indexType == IndexType.Sold)
			fileName = "data/" + listingName.toString() + "/delistings/30daysComm.txt";
		try {
			String indexStr = FileUtils.toJsonString(briefs);
			saveFileContent(fileName, indexStr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static void saveFileContent(String fileName, String content) throws Exception {
		synchronized (indexCache) {
			indexCache.put(fileName, content);
		}
		FileUtils.saveToFile(fileName, content);
	}

	public void saveResiIndex(Collection<ResiListingBrief> briefs, IndexType indexType) {
		String fileName = "data/" + listingName.toString() + "/listings/index.txt";
		if (indexType == IndexType.Sold)
			fileName = "data/" + listingName.toString() + "/delistings/30days.txt";
		try {
			String indexStr = FileUtils.toJsonString(briefs);
			saveFileContent(fileName, indexStr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static List<File> getFileList(String strPath) {
		List<File> filelist = new ArrayList<File>();
		File dir = new File(strPath);
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getName();
				if (files[i].isDirectory()) {
					getFileList(files[i].getAbsolutePath());
				} else if (fileName.endsWith("txt") && (!fileName.equals("index.txt"))
						&& (!fileName.equals("indexComm.txt"))) {
					String strFileName = files[i].getAbsolutePath();
					// System.out.println("---" + strFileName);
					filelist.add(files[i]);
				} else {
					continue;
				}
			}

		}
		return filelist;
	}

	public Map<String, Map<String, String>> getAllResiListings(List<File> filelist) {
		Map<String, Map<String, String>> listingsMap = new HashMap<>();
		Iterator<File> iterator = filelist.iterator();
		while (iterator.hasNext()) {
			File file = (File) iterator.next();
			String listingTxt;
			String fileName = "data/" + listingName.toString() + "/listings/" + file.getName();
			try {
				listingTxt = getFileContent(fileName);
				JsonNode jnode = Json.parse(listingTxt);
				HashMap<String, String> listingMap = (HashMap<String, String>) Json.fromJson(jnode, Map.class);
				//System.out.println(listingMap.toString());
				listingsMap.put(file.getName(), listingMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return listingsMap;
	}

	public int updateIndex(String[] parameters) throws Exception {
		Map<String, String> map;
		System.out.println("start to change index of " + listingName.toString());
		if (parameters.length > 0 && null != parameters) {
			map = arrayToMap(parameters);
		} else {
			System.out.println("parameter can not be null, please set it like k=v");
			return 0;
		}
		List<ResiListingBrief> resiBriefArray = getAllResiListingBriefs(IndexType.Available);
		Map<String, ResiListingBrief> resiBriefs = toMap(resiBriefArray);
		List<CommListingBrief> commBriefArray = getAllCommListingBriefs(IndexType.Available);
		Map<String, CommListingBrief> commBriefs = toMap(commBriefArray);
		System.out.println("Existing comm size:" + commBriefs.size());
		System.out.println("Existing resi size:" + resiBriefs.size());
		String propertyId = map.get("propertyId");
		if (resiBriefs.containsKey(propertyId)) {
			System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
			resiBriefs.get(propertyId).listingTimestamp = map.get("listingTimestamp");

			System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
			saveResiIndex(resiBriefs.values(), IndexType.Available);
		} else if (commBriefs.containsKey(propertyId)) {
			System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
			resiBriefs.get(propertyId).listingTimestamp = map.get("listingTimestamp");
			System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
			saveCommIndex(commBriefs.values(), IndexType.Available);
		} else {
			System.out.println("can not find the propertyId");
			return 0;
		}

		System.out.println("end to change index");
		int listingUpdated = 0;
		return listingUpdated;
	}

	public int updateStyle(String[] parameters) throws Exception {
		int updateNum = 0; 
		System.out.println("start to change index of " + listingName.toString());
		List<ResiListingBrief> resiBriefArray = getAllResiListingBriefs(IndexType.Available);
		Map<String, ResiListingBrief> resiBriefs = toMap(resiBriefArray);
		System.out.println("Existing resi size:" + resiBriefs.size());

		List<File> listingFiles = getFileList("data/" + listingName.toString() + "/listings/");
		Map<String, Map<String, String>> resiListingsMap = getAllResiListings(listingFiles);
		for (int i = 0; i < resiListingsMap.size(); i++) {
			if (!listingFiles.get(i).exists()) {
				System.out.println("listing file not found, what's wrong");
				return 0;
			}
			String fileName = listingFiles.get(i).getName();
			String propertyId = fileName.replace(".txt", "");
			if (null==fileName) {
				System.out.println("listing file name is null");
				continue;
			}
			JsonNode jnode = Json.parse(FileUtils.toJsonString(resiListingsMap.get(fileName).get("attributes")));
			HashMap<String, String> listingMap = (HashMap<String, String>) Json.fromJson(jnode, Map.class);
			//String attributes = FileUtils.toJsonString(resiListingsMap.get(propertyId).get("attributes"));
			if (listingMap.containsKey("architecturalstyle")) {
				if (resiBriefs.get(propertyId)==null) {
					System.out.println("no data,skip");
					continue;
				}
				//System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
				String style = listingMap.get("architecturalstyle");
				if (null==style||style.equals("null")) {
					System.out.println("style is null");
					continue;
				}
				//System.out.println(style);
				if (style.contains(",")) {
					String [] styles = style.split(",");
					resiBriefs.get(propertyId).styles = new String[styles.length];
					for(int j = 0;j<styles.length;j++){
						resiBriefs.get(propertyId).styles[j] = styles[j];;	
					}
				}else{
					resiBriefs.get(propertyId).styles = new String[1];
					resiBriefs.get(propertyId).styles[0] = style;						
				}

				//System.out.println(FileUtils.toJsonString(resiBriefs.get(propertyId)));
				updateNum++;
			}
		}
		//System.out.println(FileUtils.toJsonString(resiBriefs));
		saveResiIndex(resiBriefs.values(), IndexType.Available);

		System.out.println("end to change index");
		return updateNum;
	}

	public static void main(String[] args) throws Exception {
		if ((args.length > 0) && null != args) {
			listingName.append(args[0]);
			rebuildindex helper = new rebuildindex();
			// helper.updateIndex(args);
			helper.updateStyle(args);
		} else {
			System.out.println("please add parameters");
		}
	}

}
