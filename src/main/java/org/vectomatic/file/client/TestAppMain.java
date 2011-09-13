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

import org.vectomatic.dnd.DropPanel;
import org.vectomatic.dom.svg.OMSVGRect;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.OMSVGStyle;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.utils.OMSVGParser;
import org.vectomatic.dom.svg.utils.SVGConstants;
import org.vectomatic.file.File;
import org.vectomatic.file.FileList;
import org.vectomatic.file.FileReader;
import org.vectomatic.file.FileUploadExt;
import org.vectomatic.file.events.DragEnterEvent;
import org.vectomatic.file.events.DragLeaveEvent;
import org.vectomatic.file.events.DragOverEvent;
import org.vectomatic.file.events.DropEvent;
import org.vectomatic.file.events.LoadEndEvent;
import org.vectomatic.file.events.LoadEndHandler;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
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
		
		final Style style = dropPanel.getElement().getStyle();
		style.setWidth(150, Unit.PX);
		style.setHeight(150, Unit.PX);
		style.setBorderStyle(BorderStyle.SOLID);
		setBorderColor(SVGConstants.CSS_BLACK_VALUE);
		RootLayoutPanel.get().add(flowPanel);
	}
	
	private void setBorderColor(String color) {
		dropPanel.getElement().getStyle().setBorderColor(color);
	}
	
	private void processFiles(FileList files) {
		GWT.log("length=" + files.getLength());
		for (File file : files) {
			GWT.log("name=" + file.getName());
			GWT.log("urn=" + file.getUrn());
			String type = file.getType();
			GWT.log("type=" + type);
			if ("image/svg+xml".equals(type)) {
				addSvgImage(file);
			} else if ("image/png".equals(type)) {
				final FileReader reader = new FileReader();
				reader.addLoadEndHandler(new LoadEndHandler() {
					
					@Override
					public void onLoadEnd(LoadEndEvent event) {
						try {
							String result = reader.getResult();
							String url = "data:image/png;base64," + base64encode(result);
							imagePanel.add(new Image(url));
						} catch(Throwable t) {
							GWT.log("PNG loading error: ", t);
						}
						
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
					final OMSVGSVGElement svg = OMSVGParser.parse(reader.getResult());
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
		processFiles(event.getDataTransfer().getFiles());
		setBorderColor(SVGConstants.CSS_BLACK_VALUE);
		event.stopPropagation();
		event.preventDefault();
	}

}
