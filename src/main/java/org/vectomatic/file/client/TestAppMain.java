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

import org.vectomatic.dnd.DataTransferExt;
import org.vectomatic.dnd.DropPanel;
import org.vectomatic.dom.svg.OMSVGRect;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.OMSVGStyle;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.utils.OMSVGParser;
import org.vectomatic.dom.svg.utils.SVGConstants;
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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootLayoutPanel;

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
	
    interface TestAppMainBinder extends UiBinder<FlowPanel, TestAppMain> {
    }
    private static TestAppMainBinder binder = GWT.create(TestAppMainBinder.class);

	@Override
	public void onModuleLoad() {
		FlowPanel flowPanel = binder.createAndBindUi(this);
		Document document = Document.get();
		dropPanel.getElement().appendChild(document.createDivElement()).appendChild(document.createTextNode("Drop files here"));		
		RootLayoutPanel.get().add(flowPanel);
	}
	
	private void setBorderColor(String color) {
		dropPanel.getElement().getStyle().setBorderColor(color);
	}
	
	private void processFiles(FileList files) {
		GWT.log("length=" + files.getLength());
		for (File file : files) {
			GWT.log("name=" + file.getName());
			final String type = file.getType();
			GWT.log("type=" + type);
			if ("image/svg+xml".equals(type)) {
				addSvgImage(file);
			} else if (type.startsWith("image/")) {
				final FileReader reader = new FileReader();
				reader.addLoadEndHandler(new LoadEndHandler() {
					
					@Override
					public void onLoadEnd(LoadEndEvent event) {
						try {
							if (reader.getError() == null) {
								String result = reader.getStringResult();
								String url = "data:" + type + ";base64," + base64encode(result);
								final Image image = new Image();
								image.addLoadHandler(new LoadHandler() {
									
									@Override
									public void onLoad(LoadEvent event) {
										int width = image.getWidth();
										int height = image.getHeight();
										GWT.log("size=" + width + "x" + height);
										float f = 150.0f / Math.max(width, height);
										image.setPixelSize((int)(f * width), (int)(f * height));
										imagePanel.getElement().getStyle().setVisibility(Visibility.VISIBLE);
									}
								});
								imagePanel.add(image);
								imagePanel.getElement().getStyle().setVisibility(Visibility.HIDDEN);
								image.setUrl(url);
							}
						} catch(Throwable t) {
							GWT.log("PNG loading error: ", t);
						}
						
					}
				});
				
 				reader.addErrorHandler(new ErrorHandler() {
					@Override
					public void onError(ErrorEvent event) {
						FileError error = reader.getError();
						String errorDesc = "";
						if (error != null) {
							ErrorCode errorCode = error.getCode();
							if (errorCode != null) {
								errorDesc = ": " + errorCode.name();
							}
						}
						Window.alert("File loading error" + errorDesc);									
					}
 				});

				reader.readAsBinaryString(file);
			}
		}		
	}
	
	private static native String base64encode(String str) /*-{
		return $wnd.btoa(str);
	}-*/;

	
	private void addSvgImage(final File file) {
		final FileReader reader = new FileReader();
		reader.addLoadEndHandler(new LoadEndHandler() {
			
			@Override
			public void onLoadEnd(LoadEndEvent event) {
				try {
					final OMSVGSVGElement svg = OMSVGParser.parse(reader.getStringResult());
					imagePanel.add(new SVGImage(svg) {
				    	protected void onAttach() {
				    		OMSVGRect viewBox = svg.getViewBox().getBaseVal();
							if (viewBox.getWidth() == 0 || viewBox.getHeight() == 0) {
								OMSVGRect bbox = svg.getBBox();
								bbox.assignTo(viewBox);
							}
							OMSVGStyle style = svg.getStyle();
							style.setWidth(150, Unit.PX);
							style.setHeight(150, Unit.PX);
							super.onAttach();
				    	}
					});
				} catch(Throwable t) {
					GWT.log("SVG loading error: ", t);
				}
			}
		});
		reader.readAsText(file);
		
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
