package com.venky.swf.plugins.wiki.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.plugins.wiki.db.model.Page;
import com.venky.swf.plugins.wiki.views.MarkDownView;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.ModelListView;
public class PagesController extends ModelController<Page>{

	public PagesController(Path path) {
		super(path);
	}
	
	private List<Page> getLandingPages(){
		Expression exp = new Expression(getReflector().getPool(),Conjunction.AND);
		exp.add(new Expression(getReflector().getPool(),"LANDING_PAGE",Operator.EQ,true));
		exp.add(getPath().getWhereClause());
		List<Page> pages = new Select().from(Page.class).where(exp).orderBy(getReflector().getOrderBy()).execute(Page.class, MAX_LIST_RECORDS, new DefaultModelFilter<Page>(Page.class));
		return pages;
	}

	public View index(){ 
		List<Page> pages = getLandingPages();
		return super.list(pages,pages.size() < MAX_LIST_RECORDS);
	}
	
	public View show(String title){
		return view(title);
	}

    public View show(long id){
		return view(id);
	}

	@RequireLogin(false)
	public View view(String title){
		try {
			int id = Integer.valueOf(title);
			return view(id);
		}catch(NumberFormatException ex){
			List<Page> pages = findAllByTitle(title);
			if (pages.isEmpty()){
				if (!getPath().isUserLoggedOn() || getPath().isGuestUserLoggedOn()) {
					try {
						String url = URLEncoder.encode("/pages/view/"+title,"UTF-8");
						return new RedirectorView(getPath(),"","login?_redirect_to="+url);
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}else {
					//Handle empty to create empty page with right title.
					Page page = newPage();
					page.setTitle(title);
					page.setTag(title);
					return blank(page);
				}
			}else {
				pages = findAllByTitle(title);
				return list(pages,true);
			}
		}
	}

	public View blank(){
		Page page = newPage();
		return blank(page);
	}
	
	public View view(long id){
		Page page = Database.getTable(Page.class).get(id);
		return view(page);
	}
	
	protected View view(Page page){
		if (page.isAccessibleBy(getSessionUser())){
			return dashboard(createMarkdownView(getPath(),page));
		}else {
			throw new AccessDeniedException();
		}
	}

	protected HtmlView createMarkdownView(Path path, Page page) {
		return new MarkDownView(getPath(), page);
	}

	private Page newPage(){
		Page page = Database.getTable(Page.class).newRecord();
		User user = getSessionUser();
		List<UserCompany> userCompanies = user.getUserCompanies(); 
		if (userCompanies.size() > 0) {
			page.setCompanyId(userCompanies.get(0).getCompanyId());
		}
		return page;
	}
	
	private List<Page> findAllByTitle(String title){
		Expression where = new Expression(getReflector().getPool(),Conjunction.AND);
		where.add(new Expression(getReflector().getPool(),"TITLE",Operator.EQ,title));
		where.add(getPath().getWhereClause());
		Select sel = new Select().from(Page.class).where(where);
		List<Page> pages =  sel.execute(Page.class,new DefaultModelFilter<Page>(Page.class));
		Collections.sort(pages,new Comparator<Page>(){
			@SuppressWarnings("unchecked")
			TypeConverter<Integer> converter = (TypeConverter<Integer>)Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(Integer.class).getTypeConverter();
			public int compare(Page o1, Page o2) {
				Integer c1 = converter.valueOf(o1.getCompanyId());
				Integer c2 = converter.valueOf(o2.getCompanyId());
				return c2.compareTo(c1);
			}
			
		});
		return pages;
	}

	@Override
	protected void rewriteQuery(Map<String,Object> formData){
		String strQuery = StringUtil.valueOf(formData.get("q"));
		if (!ObjectUtil.isVoid(strQuery) && !strQuery.contains(":")){
			strQuery = "BODY:" +strQuery + " OR TITLE:" + strQuery + " OR TAG:" +strQuery;
		}
		formData.put("q", strQuery);
	}
	
	
	@Override
	protected View afterPersistDBView(Page page){
		return new RedirectorView(getPath(), "view/" + page.getId());
	}
	
	@Override
    protected View constructModelListView(List<Page> records,boolean isCompleteList){
		if (records.size() > 1){
			return new PageListView(getPath(), records,isCompleteList);
		}else if (records.size() == 1){
			return redirectTo("view/"+records.get(0).getId()); 
		}else {
			if (getPath().canAccessControllerAction("save")){
				return redirectTo("blank");
			}else {
				return new PageListView(getPath(), records,isCompleteList);
			}
		}
	}
    
    public class PageListView extends ModelListView<Page>{
		public PageListView(Path path, List<Page> records,boolean isCompleteList) {
			super(path, null, records,isCompleteList);
			getIncludedFields().remove("BODY");
		}
    }
    
    
}
