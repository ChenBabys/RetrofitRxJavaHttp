package com.tqkj.retrofitrxjavahttp.viewmodel;
import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.model.MainModel;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/5 23:58
 * desc : 若需要Application 则继承AndroidViewModel
 * version : 1.0
 */
public class MainViewModel extends ViewModel {
    private final LiveData<List<WangYiNewsBean>> wangyiNewsLiveData;
    //为了实现刷新数据，直接把他从构造函数中抽离出来，不再在构造函数中直接调用请求数据的方法了
    private final MainModel mainModel;
    public int page = 1;//首次为1

    //构造函数，只有第一次绑定时候会执行。
    public MainViewModel(MainModel mainModel) {
        //wangyiNewsLiveData = mainModel.getNews();//网络请求并返回
        this.mainModel = mainModel;
        wangyiNewsLiveData = refreshNews(String.valueOf(page));//首次给第一页，只会执行一次绑定，所以是首次。之后不会在执行，只会通过livedata动态观察
        ToastUtils.showShort("初次见面，请多多指教");
    }

    //基于view层获取数据的方法。
    public LiveData<List<WangYiNewsBean>> getNews() {
        return wangyiNewsLiveData;
    }

    //网络请求并返回，刷新也用它，因为model已经和livedata在构造函数中绑定了，所以只有刷新了model中的东西，livedata就会观察到更新。
    public LiveData<List<WangYiNewsBean>> refreshNews(String page) {
        return mainModel.getNews(page);//网络请求并返回
    }


    /**
     * 提前设置构造类
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final MainModel mainModel;

        public Factory() {
            this.mainModel = new MainModel();
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MainViewModel(mainModel);//设置为无参数
        }
    }

}
