package may.baseraids.config;

import java.io.File;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import may.baseraids.Baseraids;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Config {

	private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec config;
	
	static {
		config = builder.build();
	}
	
	
	public static void loadConfig(ForgeConfigSpec config, String configPath) {
		Baseraids.LOGGER.info("Loading config from " + configPath);
		final CommentedFileConfig file = CommentedFileConfig.builder(new File(configPath)).sync().autosave().writingMode(WritingMode.REPLACE).build();
		file.load();
		Baseraids.LOGGER.info("Completed loading config from " + configPath);
		config.setConfig(file);
	}
}
