/**********************************************
 * Copyright (C) 2011 Lukas laag
 * This file is part of lib-gwt-file.
 * 
 * lib-gwt-file is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * lib-gwt-file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with lib-gwt-file.  If not, see http://www.gnu.org/licenses/
 **********************************************/
package org.vectomatic.file.client;

import java.util.ArrayList;
import java.util.List;

import org.vectomatic.dnd.DataTransferExt;
import org.vectomatic.dnd.DropPanel;
import org.vectomatic.dom.svg.OMSVGRect;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.utils.DOMHelper;
import org.vectomatic.dom.svg.utils.OMSVGParser;
import org.vectomatic.dom.svg.utils.SVGConstants;
import org.vectomatic.file.Blob;
import org.vectomatic.file.ErrorCode;
import org.vectomatic.file.File;
import org.vectomatic.file.FileError;
import org.vectomatic.file.FileList;
import org.vectomatic.file.FileReader;
import org.vectomatic.file.FileUploadExt;
import org.vectomatic.file.events.ErrorEvent;
import org.vectomatic.file.events.ErrorHandler;
import org.vectomatic.file.events.LoadEndEvent;
import org.vectomatic.file.events.LoadEndHandler;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class TestAppMain implements EntryPoint {
	@UiField
	Button resetBtn;
	@UiField
	Button browseBtn;
	@UiField
	DropPanel dropPanel;
	@UiField
	FileUploadExt fileUpload;
	@UiField
	FileUploadExt customUpload;
	@UiField
	FlowPanel imagePanel;
	
	@UiField(provided=true)
	static TestAppMainBundle bundle = GWT.create(TestAppMainBundle.class);
	protected FileReader reader;
	protected List<File> readQueue;
	
    interface TestAppMainBinder extends UiBinder<FlowPanel, TestAppMain> {
    }
    private static TestAppMainBinder binder = GWT.create(TestAppMainBinder.class);
    interface TestAppMainCss extends CssResource {
    	public String imagePanel();
    	public String customUpload();
    	public String dropPanel();
    	public String thumbnail();
    	@ClassName("thumbnail-image")
		public String thumbnailImage();
    	@ClassName("thumbnail-text")
		public String thumbnailText();
    	@ClassName("txt")
		public String text();
    }
    interface TestAppMainBundle extends ClientBundle {
    	@Source("TestAppMainCss.css")
    	public TestAppMainCss css();
    }

	@Override
	public void onModuleLoad() {
		bundle.css().ensureInjected();
		FlowPanel flowPanel = binder.createAndBindUi(this);
		Document document = Document.get();
		dropPanel.getElement().appendChild(document.createDivElement()).appendChild(document.createTextNode("Drop files here"));		
		RootLayoutPanel.get().add(flowPanel);
		
		reader = new FileReader();
		reader.addLoadEndHandler(new LoadEndHandler() {
			@Override
			public void onLoadEnd(LoadEndEvent event) {
				if (reader.getError() == null) {
					if (readQueue.size() > 0) {
						File file = readQueue.get(0);
						try {
							imagePanel.add(createThumbnail(file));
						} finally {
							readQueue.remove(0);
							readNext();
						}
					}
				}
			}
		});
		
		reader.addErrorHandler(new ErrorHandler() {
			@Override
			public void onError(ErrorEvent event) {
				if (readQueue.size() > 0) {
					File file = readQueue.get(0);
					handleError(file);
					readQueue.remove(0);
					readNext();
				}
			}
		});
		readQueue = new ArrayList<File>();
	}
	
	private void handleError(File file) {
		FileError error = reader.getError();
		String errorDesc = "";
		if (error != null) {
			ErrorCode errorCode = error.getCode();
			if (errorCode != null) {
				errorDesc = ": " + errorCode.name();
			}
		}
		Window.alert("File loading error for file: " + file.getName() + "\n" + errorDesc);
	}
	
	private void setBorderColor(String color) {
		dropPanel.getElement().getStyle().setBorderColor(color);
	}
	
	private void processFiles(FileList files) {
		GWT.log("length=" + files.getLength());
		for (File file : files) {
			readQueue.add(file);
		}
		readNext();
	}
	
	private void readNext() {
		if (readQueue.size() > 0) {
			File file = readQueue.get(0);
			String type = file.getType();
			try {
				if ("image/svg+xml".equals(type)) {
					reader.readAsText(file);	
				} else if (type.startsWith("image/")) {
					reader.readAsBinaryString(file);
				} else if (type.startsWith("text/")) {
					// If the file is larger than 1kb, read only the first 1000 characters
					Blob blob = file;
					if (file.getSize() > 0) {
						blob = file.slice(0, 1000, "text/plain; charset=utf-8");
					}
					reader.readAsText(blob);
				}
			} catch(Throwable t) {
				// Necessary for FF (see bug https://bugzilla.mozilla.org/show_bug.cgi?id=701154)
				// Standard-complying browsers will to go in this branch
				handleError(file);
				readQueue.remove(0);
				readNext();
			}
		}
	}
	
	private FlowPanel createThumbnail(File file) {
		FlowPanel thumbnail = new FlowPanel();
		thumbnail.setStyleName(bundle.css().thumbnail());
		String type = file.getType();
		final String name = file.getName();
		final JsDate date = file.getLastModifiedDate();
		
		Widget image = null;
		if ("image/svg+xml".equals(type)) {
			image = createSvgImage();
		} else if (type.startsWith("image/")) {
			image = createBitmapImage(file);
		} else if (type.startsWith("text/")) {
			image = createText(file);
		}
		SimplePanel thumbnailImage = new SimplePanel(image);
		thumbnailImage.setStyleName(bundle.css().thumbnailImage());
		thumbnail.add(thumbnailImage);
		
		StringBuilder description = new StringBuilder(name);
		if (date != null) {
			description.append(" (");
			description.append(date.toLocaleDateString());
			description.append(")");
		}
		Label thumbnailText = new Label(description.toString());
		thumbnailText.setStyleName(bundle.css().thumbnailText());
		thumbnail.add(thumbnailText);
		return thumbnail;
	}
	
	private SVGImage createSvgImage() {
		GWT.log(reader.getStringResult());
		final OMSVGSVGElement svg = OMSVGParser.parse(reader.getStringResult());
		return new SVGImage(svg) {
	    	protected void onAttach() {
	    		OMSVGRect viewBox = svg.getViewBox().getBaseVal();
				if (viewBox.getWidth() == 0 || viewBox.getHeight() == 0) {
					OMSVGRect bbox = svg.getBBox();
					bbox.assignTo(viewBox);
				}
				svg.getStyle().setWidth(150, Unit.PX);
				svg.getStyle().setHeight(150, Unit.PX);
				super.onAttach();
	    	}
		};
	}
	
	private Image createBitmapImage(final File file) {
		String result = reader.getStringResult();
		String url = "data:" + file.getType() + ";base64," + DOMHelper.base64encode(result);
		final Image image = new Image();
		image.setVisible(false);
		image.addLoadHandler(new LoadHandler() {
			@Override
			public void onLoad(LoadEvent event) {
				int width = image.getWidth();
				int height = image.getHeight();
				GWT.log("size=" + width + "x" + height);
				float f = 150.0f / Math.max(width, height);
				image.setPixelSize((int)(f * width), (int)(f * height));
				image.setVisible(true);
			}			
		});
		image.setUrl(url);
		return image;			
	}

	private FlowPanel createText(final File file) {
		String result = reader.getStringResult();
		FlowPanel panel = new FlowPanel();
		panel.getElement().appendChild(Document.get().createTextNode(result));
		panel.addStyleName(bundle.css().text());
		return panel;
	}
	
	@UiHandler("browseBtn")
	public void browse(ClickEvent event) {
		customUpload.click();
	}

	@UiHandler("resetBtn")
	public void reset(ClickEvent event) {
		for (int i = imagePanel.getWidgetCount() - 1; i >= 0; i--) {
			imagePanel.remove(i);
		}
	}
	
	@UiHandler("fileUpload")
	public void uploadFile1(ChangeEvent event) {
		processFiles(fileUpload.getFiles());
	}

	@UiHandler("customUpload")
	public void uploadFile2(ChangeEvent event) {
		processFiles(customUpload.getFiles());
	}
	
	@UiHandler("dropPanel")
	public void onDragOver(DragOverEvent event) {
		// Mandatory handler, otherwise the default
		// behavior will kick in and onDrop will never
		// be called
		event.stopPropagation();
		event.preventDefault();
	}
	
	@UiHandler("dropPanel")
	public void onDragEnter(DragEnterEvent event) {
		setBorderColor(SVGConstants.CSS_RED_VALUE);
		event.stopPropagation();
		event.preventDefault();
	}
	
	@UiHandler("dropPanel")
	public void onDragLeave(DragLeaveEvent event) {
		setBorderColor(SVGConstants.CSS_BLACK_VALUE);
		event.stopPropagation();
		event.preventDefault();
	}
	
	@UiHandler("dropPanel")
	public void onDrop(DropEvent event) {
		processFiles(event.getDataTransfer().<DataTransferExt>cast().getFiles());
		setBorderColor(SVGConstants.CSS_BLACK_VALUE);
		event.stopPropagation();
		event.preventDefault();
	}

}
