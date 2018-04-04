# TvRecyclerView
用在android TV端的自定义recyclerView控件, 不支持手机端.

## ScreenShot
<p>
<img src="screenshot1.png" width="75%" />
<br/>
<br/>
<img src="screenshot2.png" width="75%" />
<br/>
<br/>
<img src="screenshot3.png" width="75%" />
</p>

## 使用步骤
1.首先在你项目的gradle配置以下命令, 引用TvRecyclerView :
```groovy
dependencies {
    ......
    compile 'com.henryblue.library:tvrecyclerview:1.0.9'
   }

```
将上面的引用命令添加到build.gradle(是module下面的, 不是项目下面的)

2. 添加TvRecyclerView到你的布局中
```
<app.com.tvrecyclerview.TvRecyclerView
        android:id="@+id/tv_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="580dp"
        app:focusDrawable="@drawable/default_focus"/>
```

3.设置LayoutManager 和 Adapter
```
 mTvRecyclerView = (TvRecyclerView) findViewById(R.id.tv_recycler_view);
 GridLayoutManager manager = new GridLayoutManager(NormalFocusActivity.this, 3);
 manager.setOrientation(LinearLayoutManager.HORIZONTAL);
 mTvRecyclerView.setLayoutManager(manager);

 int itemSpace = getResources().
                getDimensionPixelSize(R.dimen.recyclerView_item_space);
 mTvRecyclerView.addItemDecoration(new SpaceItemDecoration(itemSpace));
        
 NormalAdapter mAdapter = new NormalAdapter(this);
 mTvRecyclerView.setAdapter(mAdapter);

 mTvRecyclerView.setOnItemStateListener(new TvRecyclerView.OnItemStateListener() {
      @Override
      public void onItemViewClick(View view, int position) {
          Log.i(TAG, "you click item position: " + position);
      }

      @Override
      public void onItemViewFocusChanged(boolean gainFocus, View view, int position) {
      }
 });
 mTvRecyclerView.setSelectPadding(35, 34, 35, 38);
```

## TvRecyclerView的自定义属性

```xml
    <app.com.tvrecyclerview.TvRecyclerView
        ...
        app:scrollMode="followScroll"
        app:isAutoProcessFocus="false"
        app:focusScale="1.04f"
        app:focusDrawable="@drawable/default_focus"
        ...
        />
```


| Name | Type | Default | Description |
|:----:|:----:|:-------:|:-----------:|
|scrollMode|enum|normalScroll| 设置滑动模式 |
|isAutoProcessFocus|boolean|true| 设置是否由TvRecyclerView处理焦点 |
|focusScale|Float|1.04f| 设置item获取焦点时放大倍数. 如果设置属性'isAutoProcessFocus', 这个属性不生效, 总是默认值1.0f |
|focusDrawable|reference|null| 设置焦点框背景图, 如果设置属性'isAutoProcessFocus', 这个属性不生效 |

## TvRecyclerView提供的公共接口

| Name |       Description            |
|:----:|:---------------:|
|setSelectedScale(float)| 设置item获取焦点时放大倍数 |
|setIsAutoProcessFocus(boolean)| 设置属性'isAutoProcessFocus' |
|setFocusDrawable(Drawable)| 置焦点框背景图 |
|setItemSelected(int)| 设置指定的item获取焦点 |
|setSelectPadding(int,int,int,int)| 调整焦点框的padding值, 确保焦点框包裹住item |

此外: 因为TvRecyclerView继承RecyclerView, 所以你可以使用所有的RecyclerView的所有公共接口.
