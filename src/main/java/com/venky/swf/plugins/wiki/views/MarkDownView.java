package com.venky.swf.plugins.wiki.views;

import org.pegdown.PegDownProcessor;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.wiki.db.model.Page;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidTable;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.Panel;
import com.venky.swf.views.controls.page.layout.Panel.PanelHeading;
import com.venky.swf.views.controls.page.layout.headings.H;
import com.venky.swf.views.model.ModelListView;

public class MarkDownView extends HtmlView{
	Page page; 
	public MarkDownView(_IPath path,Page page) {
		super(path);
		this.page = page;
	}


	@Override
	protected void createBody(_IControl b) {
    	FluidTable container = new FluidTable(1);
    	b.addControl(container);
    	H h3 = new H(3);
    	h3.setText(page.getTitle());
    	container.addControl(h3);

    	container.addControl(createSearchForm(page));
    	
    	
		Div markdown = new Div();
		markdown.addClass("markdown");
		container.addControl(markdown);
		PegDownProcessor p = new PegDownProcessor();
		String html = p.markdownToHtml(StringUtil.read(page.getBody()));
		markdown.setText(html);
		
	}

	private Control createSearchForm(Page page){
		Panel contentPanel = new Panel();
    	PanelHeading headingPanel = contentPanel.createPanelHeading(); 
    	headingPanel.setTitle("Search");
    	
		ModelListView.createSearchForm(getPath(),headingPanel);
		return contentPanel;
    }
    
    private SequenceSet<HotLink> links = null; 

	@Override
	public SequenceSet<HotLink> getHotLinks(){
		if (links == null){
			links = super.getHotLinks();
			if (getPath().canAccessControllerAction("edit",String.valueOf(page.getId()))){
				HotLink edit = new HotLink();
				edit.setUrl(getPath().controllerPath()+"/edit/"+page.getId());
				edit.addControl(new Glyphicon("glyphicon-edit","Edit Page"));
            	links.add(edit);
			}
			
			if (getPath().canAccessControllerAction("blank",String.valueOf(page.getId()))){
            	HotLink create = new HotLink();
                create.setUrl(getPath().controllerPath()+"/blank");
                create.addControl(new Glyphicon("glyphicon-plus","New Page"));
            	links.add(create);
			}
			
		}
		return links;
	}
}
