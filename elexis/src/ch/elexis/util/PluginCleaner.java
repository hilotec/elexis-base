package ch.elexis.util;

import java.io.File;
import java.util.HashMap;

import ch.rgw.IO.FileTool;

public class PluginCleaner {

	public static void clean(String basedir){
		//basedir="d:/apps/elexis-1.2.0";
		HashMap<String, String> plugins=new HashMap<String, String>();
		File pluginDir=new File(basedir,"plugins");
		if(pluginDir.exists() && pluginDir.isDirectory()){
			String[] files=pluginDir.list();
			for(String file:files){
				int pos=file.lastIndexOf('_');
				if(pos!=-1){
					String basename=file.substring(0,pos);
					String exists=plugins.get(basename);
					if(exists==null){
						plugins.put(basename, file);
					}else{
						int epos=exists.lastIndexOf('_');
						String[] vExists=exists.substring(epos+1).split("\\.");
						String[] vNew=file.substring(pos+1).split("\\.");
						if(vExists.length<vNew.length){
							FileTool.deltree(pluginDir.getAbsolutePath()+File.separator+exists);
							plugins.put(basename, file);
						}
						else if(vExists.length>vNew.length){
							FileTool.deltree(pluginDir.getAbsolutePath()+File.separator+file);
						}else{
							for(int i=0;i<vNew.length;i++){
								if(vExists[i].compareTo(vNew[i])<0){
									FileTool.deltree(pluginDir.getAbsolutePath()+File.separator+exists);
									plugins.put(basename, file);
									break;
								}else if(vExists[i].compareTo(vNew[i])>0){
									FileTool.deltree(pluginDir.getAbsolutePath()+File.separator+file);
									break;
								}
							}
						}
					}
				}
			}
		}
		
		
	}
}
