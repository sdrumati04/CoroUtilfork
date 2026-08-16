package com.corosus.coroutil.loader.fabric;

import com.corosus.coroutil.util.CULog;
import com.corosus.modconfig.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Implementazione nativa Fabric del gestore di configurazione di CoroUtil.
 * 
 * COME FUNZIONA:
 * Nelle versioni Forge, CoroUtil utilizzava IConfigSpec/ForgeConfigSpec.
 * Su Fabric puro, questa classe gestisce direttamente la lettura e scrittura di file TOML
 * nella cartella 'config/' di Fabric senza librerie esterne pesanti.
 * 
 * Legge i campi annotati (@ConfigComment, @ConfigParams) tramite Java Reflection
 * e scrive un file TOML formattato e commentato comprensibile all'utente.
 */
public class ModConfigDataFabric extends ModConfigData {

    public HashMap<String, String> valsStringConfig = new HashMap<>();
    public HashMap<String, Integer> valsIntegerConfig = new HashMap<>();
    public HashMap<String, Double> valsDoubleConfig = new HashMap<>();
    public HashMap<String, Boolean> valsBooleanConfig = new HashMap<>();

    public ModConfigDataFabric(String savePath, String parStr, Class parClass, IConfigCategory parConfig) {
        super(savePath, parStr, parClass, parConfig);
    }

    private Path getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(saveFilePath + ".toml");
    }

    @Override
    public String getConfigString(String fieldName) {
        return valsStringConfig.getOrDefault(fieldName, "");
    }

    @Override
    public Integer getConfigInteger(String fieldName) {
        return valsIntegerConfig.getOrDefault(fieldName, 0);
    }

    @Override
    public Double getConfigDouble(String fieldName) {
        return valsDoubleConfig.getOrDefault(fieldName, 0.0);
    }

    @Override
    public Boolean getConfigBoolean(String fieldName) {
        return valsBooleanConfig.getOrDefault(fieldName, false);
    }

    @Override
    public <T> void setConfig(String fieldName, T obj) {
        if (obj instanceof String s) {
            valsStringConfig.put(fieldName, s);
        } else if (obj instanceof Integer i) {
            valsIntegerConfig.put(fieldName, i);
        } else if (obj instanceof Double d) {
            valsDoubleConfig.put(fieldName, d);
        } else if (obj instanceof Boolean b) {
            valsBooleanConfig.put(fieldName, b);
        }
        writeConfigFile(false);
    }

    @Override
    public void writeConfigFile(boolean resetConfig) {
        Path configFile = getConfigFile();
        try {
            if (!resetConfig && Files.exists(configFile)) {
                loadFromFile(configFile);
            }
            saveToFile(configFile);
        } catch (Exception e) {
            CULog.err("Failed to load/save config file " + configFile);
            e.printStackTrace();
        }
    }

    private void loadFromFile(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        val = val.substring(1, val.length() - 1);
                    }
                    applyParsedValue(key, val);
                }
            }
        } catch (Exception e) {
            CULog.err("Failed to read config " + file);
            e.printStackTrace();
        }
    }

    private void applyParsedValue(String key, String val) {
        try {
            Field field = configClass.getDeclaredField(key);
            Class<?> type = field.getType();
            if (type == String.class) {
                valsStringConfig.put(key, val);
                setFieldBasedOnType(key, val);
            } else if (type == int.class || type == Integer.class) {
                int intVal = Integer.parseInt(val);
                valsIntegerConfig.put(key, intVal);
                setFieldBasedOnType(key, intVal);
            } else if (type == double.class || type == Double.class) {
                double doubleVal = Double.parseDouble(val);
                valsDoubleConfig.put(key, doubleVal);
                setFieldBasedOnType(key, doubleVal);
            } else if (type == boolean.class || type == Boolean.class) {
                boolean boolVal = Boolean.parseBoolean(val);
                valsBooleanConfig.put(key, boolVal);
                setFieldBasedOnType(key, boolVal);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveToFile(Path file) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("# General mod settings\n[general]\n\n");
                Field[] fields = configClass.getDeclaredFields();
                for (Field field : fields) {
                    String name = field.getName();
                    String comment = "-";
                    ConfigComment annoComment = field.getAnnotation(ConfigComment.class);
                    if (annoComment != null && annoComment.value().length > 0) {
                        comment = annoComment.value()[0];
                    }
                    ConfigParams annoParams = field.getAnnotation(ConfigParams.class);
                    if (annoParams != null) {
                        comment = annoParams.comment();
                    }

                    writer.write("# " + comment + "\n");
                    Object obj = CoroConfigRegistry.instance().getField(configID, name);
                    if (obj instanceof String s) {
                        writer.write(name + " = \"" + s + "\"\n\n");
                        valsStringConfig.put(name, s);
                    } else if (obj instanceof Integer i) {
                        writer.write(name + " = " + i + "\n\n");
                        valsIntegerConfig.put(name, i);
                    } else if (obj instanceof Double d) {
                        writer.write(name + " = " + d + "\n\n");
                        valsDoubleConfig.put(name, d);
                    } else if (obj instanceof Boolean b) {
                        writer.write(name + " = " + b + "\n\n");
                        valsBooleanConfig.put(name, b);
                    }
                    setFieldBasedOnType(name, obj);
                }
            }
        } catch (Exception e) {
            CULog.err("Failed to write config file " + file);
            e.printStackTrace();
        }
    }
}
