package net.zschech.gwt.chat.client;

import java.io.Serializable;
import java.util.List;

import net.zschech.gwt.chat.client.StatusUpdate.Status;
import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;
import net.zschech.gwt.comet.client.CometSerializer;
import net.zschech.gwt.comet.client.SerialTypes;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

public class ChatEntryPoint implements EntryPoint {
	
	private ChatServiceAsync chatService;
	private CometClient cometClient;
	
	private String username;
	
	private HTML messages;
	private ScrollPanel scrollPanel;
	
	@SerialTypes( { ChatMessage.class, StatusUpdate.class })
	public static abstract class ChatCometSerializer extends CometSerializer {
	}
	
	@Override
	public void onModuleLoad() {
		chatService = GWT.create(ChatService.class);
		chatService.getUsername(new AsyncCallback<String>() {
			@Override
			public void onSuccess(String username) {
				if (username == null) {
					showLogonDialog();
				}
				else {
					loggedOn(username);
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				output(caught.toString(), "red");
				// assume they are not logged in
				showLogonDialog();
			}
		});
		
		FlowPanel controls = new FlowPanel();
		final ListBox status = new ListBox();
		for (Status s : Status.values()) {
			status.addItem(s.name());
		}
		status.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setStatus(Status.values()[status.getSelectedIndex()]);
			}
		});
		
		final TextBox input = new TextBox();
		Button send = new Button("Send", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				sendMessage(input.getValue());
			}
		});
		Button logout = new Button("Logout", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				logout();
			}
		});
		
		controls.add(status);
		controls.add(input);
		controls.add(send);
		controls.add(logout);
		
		DockPanel dockPanel = new DockPanel();
		messages = new HTML();
		scrollPanel = new ScrollPanel();
		scrollPanel.setHeight("250px");
		scrollPanel.add(messages);
		dockPanel.add(scrollPanel, DockPanel.CENTER);
		dockPanel.add(controls, DockPanel.SOUTH);
		
		RootPanel.get().add(dockPanel);
	}
	
	private void showLogonDialog() {
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Login");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		
		VerticalPanel verticalPanel = new VerticalPanel();
		verticalPanel.setSpacing(4);
		dialogBox.setWidget(verticalPanel);
		
		final TextBox username = new TextBox();
		verticalPanel.add(new HTML("Username:"));
		verticalPanel.add(username);
		
		Button closeButton = new Button("Logon", new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				login(username.getValue());
			}
		});
		verticalPanel.add(closeButton);
		
		dialogBox.center();
		dialogBox.show();
	}
	
	private void login(final String username) {
		chatService.login(username, new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				loggedOn(username);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				output(caught.toString(), "red");
			}
		});
	}

	private void logout() {
		chatService.logout(username, new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				cometClient.stop();
				showLogonDialog();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				output(caught.toString(), "red");
			}
		});
	}

	private void loggedOn(String username) {
		this.username = username;
		output("logged in as " + username, "silver");
		CometSerializer serializer = GWT.create(ChatCometSerializer.class);
		cometClient = new CometClient(GWT.getModuleBaseURL() + "comet", serializer, new CometListener() {
			public void onConnected(int heartbeat) {
				output("connected " + heartbeat, "silver");
			}
			
			public void onDisconnected() {
				output("disconnected", "silver");
			}
			
			public void onError(Throwable exception, boolean connected) {
				output("error " + connected + " " + exception, "red");
			}
			
			public void onHeartbeat() {
				output("heartbeat", "silver");
			}
			
			public void onRefresh() {
				output("refresh", "silver");
			}
			
			public void onMessage(List<? extends Serializable> messages) {
				for (Serializable message : messages) {
					if (message instanceof ChatMessage) {
						ChatMessage chatMessage = (ChatMessage) message;
						output(chatMessage.getUsername() + ": " + chatMessage.getMessage(), "black");
					}
					else if (message instanceof StatusUpdate) {
						StatusUpdate statusUpdate = (StatusUpdate) message;
						output(statusUpdate.getUsername() + ": " + statusUpdate.getStatus(), "green");
					}
					else {
						output("unrecognised message " + message, "red");
					}
				}
			}
		});
		cometClient.start();
	}
	
	private void sendMessage(String message) {
		chatService.send(message, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
			}
			
			@Override
			public void onFailure(Throwable caught) {
				output(caught.toString(), "red");
			}
		});
	}
	
	private void setStatus(Status status) {
		chatService.setStatus(status, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
			}
			
			@Override
			public void onFailure(Throwable caught) {
				output(caught.toString(), "red");
			}
		});
	}
	
	public void output(String text, String color) {
		DivElement div = Document.get().createDivElement();
		div.setInnerText(text);
		div.setAttribute("style", "color:" + color);
		messages.getElement().appendChild(div);
		scrollPanel.scrollToBottom();
	}
}
