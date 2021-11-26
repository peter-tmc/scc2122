package scc.functions.src.main.java.scc.data;

public class ChannelDAO {
    private String _rid; // record id
    private String _ts; // timestamp
    private String id;
    private String name;
    private String owner;
    private boolean publicChannel;
    private String[] members;

    public ChannelDAO() {
    }

    public ChannelDAO(Channel c) {
        this(c.getId(), c.getName(), c.getOwner(), c.isPublicChannel(), c.getMembers());
    }

    public ChannelDAO(String id, String name, String owner, boolean publicChannel, String[] members) {
        super();
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.publicChannel = publicChannel;
        this.members = members;
    }

    public String get_rid() {
        return this._rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return this._ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublicChannel() {
        return this.publicChannel;
    }

    public void setPublicChannel(boolean publicChannel) {
        this.publicChannel = publicChannel;
    }

    public String[] getMembers() {
        return this.members;
    }

    public void setMembers(String[] members) {
        this.members = members;
    }

    @Override
	public String toString() {
		return "ChannelDAO [_rid=" + _rid + ", _ts=" + _ts + "id=" + id + ", name=" + name + ", owner=" 
            + owner + ", publicChannel=" + publicChannel + ", members=" + members + "]";
	}
}