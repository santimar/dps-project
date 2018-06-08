package server.data;

import util.Log;

import java.util.*;

public class Stats {
    private static Stats instance = new Stats();
    private final List<Stat> globals = new ArrayList<>();
    private final Map<String, List<Stat>> locals = new HashMap<>();

    private Stats() {
    }

    public static Stats getInstance() {
        return instance;
    }

    public void addGlobal(Stat stat) {
        synchronized (globals) {
            globals.add(stat);
        }
    }

    public void addLocal(String nodeId, Stat stat) {
        synchronized (locals) {
            List<Stat> list = locals.get(nodeId);
            if (list == null) {
                list = new ArrayList<>();
                locals.put(nodeId, list);
            }
            list.add(stat);
        }
    }

    public List<Stat> getGlobals(int amount) {
        synchronized (globals) {
            if (globals.size() == 0)
                return null;
            if (globals.size() < amount) {
                Log.d("Requested %s stats but only available %s", amount, globals.size());
                return new ArrayList<>(globals);
            }
            return new ArrayList<>(globals.subList(globals.size() - amount, globals.size()));
        }
    }

    public List<Stat> getLocals(String nodeId, int amount) {
        synchronized (locals) {
            List<Stat> localStats = locals.get(nodeId);
            if (localStats == null)
                return null;
            if (localStats.size() < amount) {
                Log.d("Requested %s stats but only available %s", amount, localStats.size());
                return new ArrayList<>(localStats);
            }
            return new ArrayList<>(localStats.subList(localStats.size() - amount, localStats.size()));
        }
    }

    public Map<String, List<Stat>> getLocals(int amount) {
        Map<String, List<Stat>> lists;
        synchronized (locals) {
            lists = new HashMap<>(locals.size());
            Set<String> keySet = locals.keySet();
            for (String key : keySet) {
                lists.put(key, getLocals(key, amount));
            }
        }
        return lists;
    }
}
