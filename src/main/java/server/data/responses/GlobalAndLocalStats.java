package server.data.responses;

import server.data.Stat;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class GlobalAndLocalStats {
    private List<Stat> globals;
    private Map<String, List<Stat>> locals;

    public GlobalAndLocalStats() {
    }

    public GlobalAndLocalStats(List<Stat> globals, Map<String, List<Stat>> locals) {
        this.globals = globals;
        this.locals = locals;
    }

    public List<Stat> getGlobals() {
        return globals;
    }

    public void setGlobals(List<Stat> globals) {
        this.globals = globals;
    }

    public Map<String, List<Stat>> getLocals() {
        return locals;
    }

    public void setLocals(Map<String, List<Stat>> locals) {
        this.locals = locals;
    }
}
