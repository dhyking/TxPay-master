package com.jszf.txsystem.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chanven.lib.cptr.PtrClassicFrameLayout;
import com.chanven.lib.cptr.PtrDefaultHandler;
import com.chanven.lib.cptr.PtrFrameLayout;
import com.chanven.lib.cptr.recyclerview.RecyclerAdapterWithHF;
import com.jszf.txsystem.MyApplication;
import com.jszf.txsystem.R;
import com.jszf.txsystem.activity.BillDetailsActivity;
import com.jszf.txsystem.adapter.BillInfomationAdapter;
import com.jszf.txsystem.bean.BillBean;
import com.jszf.txsystem.bean.BillInfo;
import com.jszf.txsystem.core.ApiRequestStores;
import com.jszf.txsystem.core.HttpHelper;
import com.jszf.txsystem.core.mvp.base.BaseFragment;
import com.jszf.txsystem.ui.DividerItemDecoration;
import com.jszf.txsystem.util.AcquireTimeNode;
import com.jszf.txsystem.util.Constant;
import com.jszf.txsystem.util.GetTimeUtil;
import com.jszf.txsystem.util.MD5Utils;
import com.jszf.txsystem.util.ParaUtils;
import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.config.DefaultConfig;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class BillFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener,
        OnDateSetListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    @BindView(R.id.tv_bill_title)
    TextView mTvBillTitle;
    @BindView(R.id.tv_bill_filter)
    TextView mTvBillFilter;
    @BindView(R.id.rl_bill_title)
    RelativeLayout mRlBillTitle;
    @BindView(R.id.recycleView_bill)
    RecyclerView mRecycleViewBill;
    @BindView(R.id.ptr_bill_frame)
    PtrClassicFrameLayout mPtrBillFrame;
    @BindView(R.id.rl_empty)
    RelativeLayout mRlEmpty;
    private int page;   //请求页数
    private String dealCode;
    private int totalPage;
    private int total;
    private List<BillInfo> list;
    private String type = "1,2,3,5,9";
    private SimpleDateFormat sf;
    private PopupWindow popupWindow;
    private StringBuffer sb;
    private Handler handler = new Handler();
    private BillInfomationAdapter mAdapter;
    private StringBuffer sb2;
    private String status = "0,1,2,6";
    private String time;
    private boolean isSelectDate;
    private long start;
    private long end;
    private RecyclerAdapterWithHF mAdapterWithHF;
    private boolean isAnd = false;
    private boolean noUpdate = false;
    private TextView mTvSelectTime;
    private String[] array = new String[]{"订单支付中", "支付成功", "支付失败", "未支付", "微信", "支付宝",
            "百度钱包", "银联", "翼支付"};
    long fiftyYears = 50L * 365 * 1000 * 60 * 60 * 24L;
    long twentYears = 20L * 365 * 1000 * 60 * 60 * 24L;
    private int refundCount;
    private String typeString = "";
    private String statusString = "";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Observable mObservable;
    private CompositeSubscription compositeSubscription;
    private View mView;

    public BillFragment() {
        // Required empty public constructor
    }


    private void iniRequest() {
        requestBillData(1);
    }

    private void requestBillData(int page) {
        ApiRequestStores apiStores = HttpHelper.getInstance().getRetrofit().create(ApiRequestStores.class);
        mObservable = apiStores.requestForBillinfo(getRequestParams(page))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        compositeSubscription = new CompositeSubscription();
        compositeSubscription.add(mObservable.subscribe(new Subscriber<BillBean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.d("BillFragment", "e:" + e);
            }

            @Override
            public void onNext(BillBean billBean) {
                try {
                    dealCode = billBean.getDealCode();
                    if (!dealCode.equals(Constant.DEAL_CODE_SUCCESS)) {
                        return;
                    }
                    BillBean.Data data = billBean.getData();
                    if (data == null) {
                        return;
                    }
                    totalPage = data.getTotalPage();
                    total = data.getTotal();
                    List<BillBean.Data.Rows> rows = data.getRows();
                    Log.d("BillActivity", "rows == null:" + (rows == null));
                    Log.d("BillActivity", "rows.size:" + (rows.size()));
                    if (rows == null || rows.size() == 0) {
                        mPtrBillFrame.setLoadMoreEnable(false);
                        if (isAnd) {
                            noUpdate = true;
                            mRlEmpty.setVisibility(View.GONE);
                            mPtrBillFrame.setVisibility(View.VISIBLE);
                        } else {
                            mRlEmpty.setVisibility(View.VISIBLE);
                            mPtrBillFrame.setVisibility(View.GONE);
                            return;
                        }
                    } else {
                        mRlEmpty.setVisibility(View.GONE);
                        mPtrBillFrame.setVisibility(View.VISIBLE);
                    }
                    if (noUpdate) {
                        Log.d("BillActivity", "noUpdate:" + noUpdate);
                        return;
                    }
                    list = new ArrayList<>();
                    for (int i = 0; i < rows.size(); i++) {
                        BillInfo mBillInfo = new BillInfo();
                        BillBean.Data.Rows Rows = rows.get(i);
                        mBillInfo.setPayTime(Rows.getOrderTime());
                        mBillInfo.setPayAmount(Rows.getOrderAmount());
                        mBillInfo.setPayState(Rows.getStatus());
                        mBillInfo.setPayType(Rows.getPayType());
                        mBillInfo.setOrderNo(Rows.getMOrderNo());
                        mBillInfo.setTxOrderNo(Rows.getTxOrderNo());
                        List<BillBean.Data.Rows.Refund> refund = Rows.getBacks();
                        if (refund != null) {
                            refundCount = refund.size();
                            mBillInfo.setRefundCount(refundCount);
                            for (int j = 0; j < refund.size(); j++) {
                                mBillInfo.setRefundState(refund.get(j).getStatus());
                            }
                        }
                        list.add(mBillInfo);
                    }

                    if (page <= totalPage) {
                        if (page == 1) {
                            mAdapter.setData(list);
                            Log.d("BillActivity", "list.size():" + list.size());
                            Log.d("BillActivity", list.get(0).getPayTime());
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mAdapter.addData(list);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));


    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            switch (buttonView.getId()) {
                case R.id.checkbox_weixin:
                    sb.append("2,");
                    Log.d("BillActivity", "1----------------微信");
                    break;
                case R.id.checkbox_weialipay:
                    sb.append("1,");
                    break;
                case R.id.checkbox_bdqb:
                    sb.append("3,");
                    break;
                case R.id.checkbox_yinlian:
                    sb.append("5,");
                    break;
                case R.id.checkbox_yzf:
                    sb.append("9,");
                    break;
                case R.id.checkbox_dealing:
                    sb2.append("0,");
                    break;
                case R.id.checkbox_deal_success:
                    sb2.append("1,");
                    break;
                case R.id.checkbox_deal_fail:
                    sb2.append("2,");
                    break;
                case R.id.checkbox_unDeal:
                    sb2.append("6,");
                    break;
            }
        } else {
            Log.d("BillActivity", "2----------------微信");
            sb.append("");
        }
    }

    private void initListener() {
        mTvBillFilter.setOnClickListener(this);
    }

    private void setAdapter() {
        mRlEmpty.setVisibility(View.GONE);
        mPtrBillFrame.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecycleViewBill.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecycleViewBill.addItemDecoration(dividerItemDecoration);
        mAdapter = new BillInfomationAdapter(getActivity());
        mAdapterWithHF = new RecyclerAdapterWithHF(mAdapter);
        mRecycleViewBill.setAdapter(mAdapterWithHF);
        mPtrBillFrame.setLastUpdateTimeRelateObject(AcquireTimeNode.getCurrentTimeToString());
        end = System.currentTimeMillis();
        long m = end - start;
        Log.d("BillActivity", "时间差:" + m);
        mAdapterWithHF.setOnItemClickListener((adapter, vh, position) -> {
            BillInfo data = (BillInfo) vh.itemView.getTag();
            Intent in = new Intent(getActivity(), BillDetailsActivity.class);
            in.putExtra("data", data);
            in.putExtra("merchantInfo", MyApplication.mMerchant);
            startActivity(in);
        });
    }


    @NonNull
    private HashMap<String, String> getRequestParams(int mPage) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("service", "offBatchSearchOrderDetail");
        map.put("version", "V1.0");
        map.put("merchantNo", MyApplication.merchantNo);
        map.put("page", "" + mPage);
        map.put("payType", type);
        map.put("payMethod", "1");
        map.put("status", status);
        if (!TextUtils.isEmpty(time)) {
            Log.d("BillActivity", "0----------------" + time);
            if (isSelectDate) {
                map.put("orderDate", time);
            }
        }
        map.put("sign", MD5Utils.MD5(ParaUtils.createLinkString(map) + MyApplication.MD5key));
        return map;
    }
    //初始化啥下拉刷新和下拉加载
    private void initRefreshOrLoadMore() {

        mPtrBillFrame.postDelayed(() -> mPtrBillFrame.autoRefresh(false), 150);
        mPtrBillFrame.setPtrHandler(new PtrDefaultHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                handler.postDelayed(() -> {
                    isAnd = false;
                    page = 1;
//                    mvpPresenter.requestBillInfo(getRequestParams(page));
                    requestBillData(page);
                    mAdapterWithHF.notifyDataSetChanged();
                    mPtrBillFrame.refreshComplete();
                    mPtrBillFrame.setLoadMoreEnable(true);

                }, 1000);
            }
        });
        mPtrBillFrame.setOnLoadMoreListener(() -> handler.postDelayed(() -> {
            isAnd = true;
//            mvpPresenter.requestBillInfo(getRequestParams(++page));
            requestBillData(++page);
            mAdapterWithHF.notifyDataSetChanged();
            mPtrBillFrame.loadMoreComplete(true);
        }, 1000));
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_bill, container, false);
        }
        ButterKnife.bind(this, mView);
        setStatus();
        setAdapter();
        initListener();
        iniRequest();
        initRefreshOrLoadMore();
        return mView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_bill_filter:
                if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                    return;
                } else {
                    sb = new StringBuffer();
                    sb2 = new StringBuffer();
                    setPopupWindow();
                    popupWindow.showAsDropDown(view, 0, 2);
                }
                break;
            case R.id.txt_time:
                showPop();
                break;
            case R.id.txt_submit:
                popupWindow.dismiss();
                if (TextUtils.isEmpty(time)) {
                    isSelectDate = false;
                    mTvSelectTime.setText("选择日期");
                } else {
                    isSelectDate = true;
                }
                if (TextUtils.isEmpty(typeString) || typeString.equals(",")) {
                    type = "1,2,3,5,9";
                } else {
                    type = typeString;
                }
                if (TextUtils.isEmpty(statusString) || statusString.equals(",")) {
                    status = "0,1,2,6";
                } else {
                    status = statusString;
                }
                if (judgeNetWord(getActivity())) {
                    page = 1;
                    noUpdate = false;
                    requestBillData(page);
                    statusString = "";
                    typeString = "";
                    time = null;
                }
                break;
        }
    }

    /**
     * 弹出选择时间框
     */
    private void showPop() {
        DefaultConfig.TITLE = "选择日期";
        TimePickerDialog mDialogYearMonthDay = new TimePickerDialog.Builder()
                .setType(Type.YEAR_MONTH_DAY)
                .setCallBack(this)
                .setMinMillseconds(System.currentTimeMillis() - twentYears)
                .setMaxMillseconds(System.currentTimeMillis() + fiftyYears)
                .setThemeColor(getResources().getColor(R.color.statusBar))
                .setWheelItemTextNormalColor(getResources().getColor(R.color.timetimepicker_default_text_color))
                .setWheelItemTextSelectorColor(getResources().getColor(R.color.blue))
                .setWheelItemTextSize(18)
                .build();
        mDialogYearMonthDay.show(getActivity().getSupportFragmentManager(), "year_month_day");
    }

    /**
     * 弹出筛选框
     */
    public void setPopupWindow() {
        final View parentView = getActivity().getLayoutInflater().inflate(R.layout.toast_view_popwindow,
                null, false);
        ListView lv = (ListView) parentView.findViewById(R.id.lv_pop);
        mTvSelectTime = (TextView) parentView.findViewById(R.id.txt_time);
        TextView tx_submit = (TextView) parentView.findViewById(R.id.txt_submit);
        tx_submit.setOnClickListener(this);
        // 创建PopupWindow实例,定义好宽度和高度
        DisplayMetrics metric = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // 屏幕宽度（像素）
        popupWindow = new PopupWindow(parentView, width / 2, ViewGroup.LayoutParams.WRAP_CONTENT);
        // 设置动画效果 [R.style.AnimationFade 是自己事先定义好的]
        ColorDrawable cd = new ColorDrawable(getResources().getColor(R.color.white));
        popupWindow.setBackgroundDrawable(cd);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);  //设置点击屏幕其它地方弹出框消失
        mTvSelectTime.setOnClickListener(this);
        tx_submit.setOnClickListener(this);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            list.add(array[i]);
        }
        PopWindowAdapter adapter = new PopWindowAdapter(getActivity(), list);
        lv.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

    @Override
    public void onDateSet(TimePickerDialog timePickerView, long millseconds) {
//        time = getDateToString(millseconds);
        time = GetTimeUtil.getDateToString(millseconds);
        Log.d("BillActivity", "time:" + time);
    }
    public String getDateToString(long time) {
        Date d = new Date(time);
        sf = new SimpleDateFormat("yyyy-MM-dd");
        return sf.format(d);
    }

    //筛选选项适配器
    class PopWindowAdapter extends BaseAdapter {
        private Context context;
        private List<String> dataList;
        private LayoutInflater inflater;
        ArrayList<Boolean> checkedItem = new ArrayList<Boolean>();

        public PopWindowAdapter(Context context, List<String> dataList) {
            this.context = context;
            this.dataList = dataList;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int i) {
            return dataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            final ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.view_popwindow_item, null);
                holder = new ViewHolder();
                holder.checkBox = (CheckBox) view.findViewById(R.id.checkbox);
                final int p = i;
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    switch (p) {
                        case 0:
                            if (holder.checkBox.isChecked()) {
//                                        sb2.append("0,");
                                statusString = statusString + "0,";
                                Log.d("PopWindow", "1---------statusString:" + statusString);

                            } else {
                                statusString = statusString.replace("0", "");
                                Log.d("PopWindow", "2---------statusString:" + statusString);
                            }
                            break;
                        case 1:
                            if (holder.checkBox.isChecked()) {
                                statusString = statusString + "1,";
                            } else {
                                statusString = statusString.replace("1,", "");
                            }
                            break;
                        case 2:
                            if (holder.checkBox.isChecked()) {
                                statusString = statusString + "2,";
                            } else {
                                statusString = statusString.replace("2,", "");
                            }
                            break;
                        case 3:
                            if (holder.checkBox.isChecked()) {
                                statusString = statusString + "6,";
                            } else {
                                statusString = statusString.replace("6,", "");
                            }
                            break;
                        case 4:
                            if (holder.checkBox.isChecked()) {
                                typeString = typeString + "2,";
                            } else {
                                typeString = typeString.replace("2,", "");
                            }
                            break;
                        case 5:
                            if (holder.checkBox.isChecked()) {
                                typeString = typeString + "1,";
                            } else {
                                typeString = typeString.replace("1,", "");
                            }
                            break;
                        case 6:
                            if (holder.checkBox.isChecked()) {
                                typeString = typeString + "3,";
                            } else {
                                typeString = typeString.replace("3,", "");
                            }
                            break;
                        case 7:
                            if (holder.checkBox.isChecked()) {
                                typeString = typeString + "5,";
                                Log.d("PopWindow", "5-1---------typeString:" + typeString);

                            } else {
                                typeString = typeString.replace("5,", "");
                                Log.d("PopWindow", "5-1---------typeString:" + typeString);
                            }
                            break;
                        case 8:
                            if (holder.checkBox.isChecked()) {
                                typeString = typeString + "9,";
                                Log.d("PopWindow", "9-1---------typeString:" + typeString);

                            } else {
                                typeString = typeString.replace("9,", "");
                                Log.d("PopWindow", "9-1---------typeString:" + typeString);

                            }
                            break;
                    }
                });
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setText(dataList.get(i));
            return view;
        }

        private class ViewHolder {
            CheckBox checkBox;
        }

    }
}
