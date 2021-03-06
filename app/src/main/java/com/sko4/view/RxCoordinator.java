package com.sko4.view;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sko4.BaseActivity;
import com.sko4.R;
import com.sko4.RxUtil;
import com.sko4.Utils;
import com.sko4.di.component.AppComponent;
import com.sko4.di.component.DaggerEventsComponent;
import com.sko4.di.component.EventsComponent;
import com.sko4.di.module.EventsModule;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Coordinator layout with rx components.
 * Created by Mayboroda on 6/10/16.
 */
public abstract class RxCoordinator<T, A extends BaseActivity> extends CoordinatorLayout implements Action1<T>{

    protected final CompositeSubscription subscriptions = new CompositeSubscription();
    protected final PublishSubject<String> eventSubject = PublishSubject.create();

    protected EventsComponent component;

    @Bind(R.id.progress)            ProgressBar progressBar;
    @Bind(R.id.switcher)            StateViewSwitcher switcher;
    @Bind(R.id.error_message)       TextView error;

    public RxCoordinator(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            AppComponent appComponent = getActivity().getAppComponent();
            component = DaggerEventsComponent.builder()
                    .appComponent(appComponent)
                    .eventsModule(new EventsModule())
                    .build();
        }
    }

    public A getActivity() { return ((A)getContext()); }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
        error.setTypeface(Utils.typeface(getContext(), Utils.ROBOTO_LIGHT));
        error.setText(getContext().getText(R.string.error_nothing));
        progressBar.setIndeterminateDrawable(new IndeterminateProgressDrawable(getContext()));
        switcher.setDisplayedChildId(R.id.progress);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscriptions.add(eventSubject
                .flatMap(API_OBSERVER)
                .subscribe(this));
        if (switcher.getDisplayedChildId() != R.id.progress) {
            switcher.setDisplayedChildId(R.id.progress);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    public abstract Observable<T> createObservable(String value);

    private Func1<String, Observable<T>> API_OBSERVER = new Func1<String, Observable<T>>() {
        @Override
        public Observable<T> call(String id) {
            return RxUtil.applySchedulers(createObservable(id))
                    .doOnError(ERROR)
                    .onErrorResumeNext(Observable.<T>empty());
        }
    };

    private Action1<Throwable> ERROR = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            switcher.setDisplayedChildId(R.id.error_message);
        }
    };

}
