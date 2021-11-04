package scc.data;

public class Channel {
    private String _rid; //record id
	private String _ts; //timestamp
    private String id;
    private String name;
    private boolean isPublic;
    private String ownerID;
    private String[] msgArray;
    private String[] memberList;

    public Channel(String id, String name, boolean isPublic, String ownerID, String[] msgArray, String[] memberList) {
        super();
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.ownerID = ownerID;
        this.msgArray = msgArray;
        this.memberList = memberList;
    }

    public String get_rid() {
		return _rid;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public String get_ts() {
		return _ts;
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

    public boolean isIsPublic() {
        return this.isPublic;
    }

    public boolean getIsPublic() {
        return this.isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getOwnerID() {
        return this.ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public String[] getMsgArray() {
        return this.msgArray;
    }

    public void setMsgArray(String[] msgArray) {
        this.msgArray = msgArray;
    }

    public String[] getMemberList() {
        return this.memberList;
    }

    public void setMemberList(String[] memberList) {
        this.memberList = memberList;
    }

}
