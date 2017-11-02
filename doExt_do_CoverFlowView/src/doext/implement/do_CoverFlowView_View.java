package doext.implement;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoUIModule;
import doext.define.do_CoverFlowView_IMethod;
import doext.define.do_CoverFlowView_MAbstract;
import doext.ui.FancyCoverFlow;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_CoverFlowView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_CoverFlowView_View extends FancyCoverFlow implements DoIUIModuleView, do_CoverFlowView_IMethod, AdapterView.OnItemSelectedListener, OnItemClickListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_CoverFlowView_MAbstract model;
	private MyFancyCoverFlowAdapter mAdapter;
	private int currentItem;
	private boolean isLooping = false;
	private DoIListData mData;

	public do_CoverFlowView_View(Context context) {
		super(context);
		mAdapter = new MyFancyCoverFlowAdapter();
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_CoverFlowView_MAbstract) _doUIModule;
		this.setUnselectedScale(0.8f);
		this.setScaleDownGravity(0.5f);
		this.setSpacing((int) (30 * model.getXZoom()));
		this.setOnItemSelectedListener(this);
		this.setOnItemClickListener(this);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("templates")) {
			try {
				mAdapter.initTemplates(_changedValues.get("templates").split(","));
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析templates错误： \t", e);
			}
		}
		if (_changedValues.containsKey("index")) {
			currentItem = DoTextHelper.strToInt(_changedValues.get("index"), 0);
			setSelection();
		}
		if (_changedValues.containsKey("spacing")) {
			int spacing = (int) (DoTextHelper.strToInt(_changedValues.get("spacing"), 30) * model.getXZoom());
			this.setSpacing(spacing);
		}

		if (_changedValues.containsKey("looping")) {
			isLooping = DoTextHelper.strToBool(_changedValues.get("looping"), false);
		}
	}

	private void setSelection() {
		if (currentItem < 0) {
			currentItem = 0;
		}
		if (mData != null && mData.getCount() > 0) {
			int _maxCount = mData.getCount() - 1;
			if (mAdapter != null && currentItem > _maxCount) {
				currentItem = _maxCount;
			}
			int _currentItem = currentItem;
			if (isLooping) {
				_currentItem = getRealPosition(currentItem);
			}
			this.setSelection(_currentItem);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		//...do something
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		//...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 绑定视图模板数据；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "data", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("doCoverFlowView 未指定 data参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("doCoverFlowView data参数无效！");
		if (_multitonModule instanceof DoIListData) {
			mData = (DoIListData) _multitonModule;
			mAdapter.bindData(mData);
			this.setAdapter(mAdapter);
			setSelection();
		}
	}

	//当looping = true 时，计算出一个合适的position
	private int getRealPosition(int _index) {
		int _centerPos = Integer.MAX_VALUE / 2;
		int _count = mData.getCount(); //获取数据条数
		_centerPos = (_centerPos / _count) * _count + _index;

		return _centerPos;
	}

	/**
	 * 刷新数据；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void refreshItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		mAdapter.notifyDataSetChanged();
		setSelection();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		position = mAdapter.getPosition(position);
		DoInvokeResult invokeResult = new DoInvokeResult(model.getUniqueKey());
		invokeResult.setResultInteger(position);
		try {
			currentItem = position;
			model.setPropertyValue("index", position + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		model.getEventCenter().fireEvent("indexChanged", invokeResult);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	class MyFancyCoverFlowAdapter extends BaseAdapter {

		private String[] uiTemplates = new String[0];
		private DoIListData data;

		public MyFancyCoverFlowAdapter() {

		}

		public void bindData(DoIListData _listData) {
			this.data = _listData;
		}

		public void initTemplates(String[] templates) throws Exception {
			uiTemplates = templates;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			try {
				int pos = getPosition(position);
				JSONObject childData = (JSONObject) data.getData(pos);
				DoIUIModuleView _doIUIModuleView = null;
				int _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), 0);
				if (_index >= uiTemplates.length || _index < 0) {
					DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
					_index = 0;
				}
				String templatePath = uiTemplates[_index];
				if (convertView == null) {
					DoUIModule uiModule = DoServiceContainer.getUIModuleFactory().createUIModuleBySourceFile(templatePath, model.getCurrentPage(), true);
					_doIUIModuleView = uiModule.getCurrentUIModuleView();
				} else {
					_doIUIModuleView = (DoIUIModuleView) convertView;
				}
				if (_doIUIModuleView != null) {
					_doIUIModuleView.getModel().setModelData(childData);
					view = (View) _doIUIModuleView;
					ViewGroup.LayoutParams params = new Gallery.LayoutParams((int) _doIUIModuleView.getModel().getRealWidth(), (int) _doIUIModuleView.getModel().getRealHeight());
					view.setLayoutParams(params);
				}
			} catch (JSONException e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("getCoverFlowItem：" + e.getMessage() + "\n", e);
			}
			if (view == null) {
				return new View(getContext());
			}
			return view;
		}

		@Override
		public int getCount() {
			return isLooping ? Integer.MAX_VALUE : getLength();
		}

		private int getLength() {
			if (data == null) {
				return 0;
			}
			return data.getCount();
		}

		public int getPosition(int position) {
			int len = getLength();
			if (len == 0) {
				return 0;
			}
			int newPos = position % len;
			return isLooping ? newPos : position;
		}

		@Override
		public Object getItem(int position) {
			try {
				return data.getData(position);
			} catch (JSONException e) {
				DoServiceContainer.getLogEngine().writeError("do_coverFlow_View getItem \n\t", e);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		doCoverFlowView_Touch(position);
	}

	private void doCoverFlowView_Touch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("index", mAdapter.getPosition(position));
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		this.model.getEventCenter().fireEvent("touch", _invokeResult);
	}

}