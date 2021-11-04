package scc.data;

public class Message {
    private String id;
	private String text;
    private String senderID;
    private String receiverID;
    private String channelID;
    private String replyingID;
	private String mediaID;

	public Message(String id, String text, String senderID, String receiverID, String channelID, String replyingID, String mediaID) {
		super();
		this.id = id;
		this.text = text;
		this.senderID = senderID;
		this.receiverID = receiverID;
		this.channelID = channelID;
		this.replyingID = replyingID;
		this.mediaID = mediaID;
	}


	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getSenderID() {
		return this.senderID;
	}

	public void setSenderID(String senderID) {
		this.senderID = senderID;
	}

	public String getReceiverID() {
		return this.receiverID;
	}

	public void setReceiverID(String receiverID) {
		this.receiverID = receiverID;
	}

	public String getChannelID() {
		return this.channelID;
	}

	public void setChannelID(String chanID) {
		this.channelID = chanID;
	}

	public String getReplyingID() {
		return this.replyingID;
	}

	public void setReplyingID(String replyingID) {
		this.replyingID = replyingID;
	}

	public String getMediaID() {
		return this.mediaID;
	}

	public void setMediaID(String mediaID) {
		this.mediaID = mediaID;
	}
	
	@Override
	public String toString() {
		return "Message [id=" + id + ", text=" + text + ", sender=" + senderID + ", receiver=" + receiverID + ", channel="
				+ channelID  + ", replying=" + replyingID + ", mediaID=" + mediaID + "]";
	}
	
}
