package alisha.digipodium.news_collector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import alisha.digipodium.news_collector.adaptor.Adaptor;
import alisha.digipodium.news_collector.api.ApiClient;
import alisha.digipodium.news_collector.api.ApiInterface;
import alisha.digipodium.news_collector.models.Article;
import alisha.digipodium.news_collector.models.News;
import alisha.digipodium.news_collector.utils.Utils;
import okhttp3.internal.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private RelativeLayout errorLayout;
    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button btnRetry;

    public static final String API_KEY = "52c45d7ad60143b1af6f3260eb2c3b21";
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<Article> articles = new ArrayList<>();
    private Adaptor adaptor;
    private String TAG = MainActivity.class.getSimpleName();
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView newHeadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        newHeadline = findViewById(R.id.newsHeadline);
        recyclerView = findViewById(R.id.rv);
        layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        onLoadSwipeRefresh("");

        errorLayout = findViewById(R.id.errorLayout);
        errorImage = findViewById(R.id.errorImage);
        errorTitle = findViewById(R.id.errorTitle);
        errorMessage = findViewById(R.id.errorMessage);
        btnRetry = findViewById(R.id.btnRetry);
    }

    public void LoadJson(final String keyword){
        errorLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        String country = Utils.getCountry();
        String language= Utils.getLanguage();

        Call<News> call;
        if (keyword.length() > 0){
            call = apiInterface.getNewsSearch(keyword,language,"publishedAt",API_KEY);
        }else {
            call = apiInterface.getNews(country,API_KEY);
        }
        call.enqueue(new Callback<News>() {
            @Override
            public void onResponse(Call<News> call, Response<News> response) {
                if (response.isSuccessful() && response.body().getArticle() != null){
                    if (!articles.isEmpty()){
                        articles.clear();
                    }
                    articles = response.body().getArticle();
                    adaptor = new Adaptor(articles,MainActivity.this);
                    recyclerView.setAdapter(adaptor);
                    adaptor.notifyDataSetChanged();

                    initListener();

                    newHeadline.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);

                }else {
                    newHeadline.setVisibility(View.INVISIBLE);
                    swipeRefreshLayout.setRefreshing(false);

                    String errorCode;
                    switch (response.code()){
                        case 404:
                            errorCode = "404 not Found";
                            break;
                        case 500:
                            errorCode = "500 Server broken";
                            break;
                        default:
                            errorCode = "unknown error";
                            break;
                    }
                    showErrorMessage(R.drawable.error1,
                            "No Result",
                            "Please Try again\n"+
                                    errorCode);
                }
            }

            @Override
            public void onFailure(Call<News> call, Throwable t) {
                newHeadline.setVisibility(View.INVISIBLE);
                swipeRefreshLayout.setRefreshing(false);
                showErrorMessage(R.drawable.error1,
                        "Oops...!",
                        "Network Failure, Please Try again\n"+
                                t.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater= getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint("Search Latest News...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 2){
                    onLoadSwipeRefresh(query);
                }else {
                    Toast.makeText(MainActivity.this, "Type more than two letters!", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                LoadJson(s);
                return false;
            }
        });
        searchMenuItem.getIcon().setVisible(false,false);
        return true;
    }

    private void initListener(){
        adaptor.setOnItemClickListener(new Adaptor.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                ImageView imageView = view.findViewById(R.id.img);
                Intent intent = new Intent(MainActivity.this, NewsDetailActivity.class);
                Article article = articles.get(position);
                intent.putExtra("url",article.getUrl());
                intent.putExtra("title",article.getTitle());
                intent.putExtra("img",article.getUrlToImage());
                intent.putExtra("date",article.getPublishedAt());
                intent.putExtra("source",article.getSource().getName());
                intent.putExtra("author",article.getAuthor());
                /*Pair<View, String> pair = Pair.create((View)imageView, ViewCompat.getTransitionName(imageView));
                ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        MainActivity.this,
                        pair
                );
                startActivity(intent,activityOptionsCompat.toBundle());*/
            }
        });
    }

    @Override
    public void onRefresh() {
        LoadJson("");
    }

    private void onLoadSwipeRefresh(final String keyword){
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                LoadJson(keyword);
            }
        });
    }

    private void showErrorMessage(int imageView,String title,String message){
        if(errorLayout.getVisibility() == View.GONE){
            errorLayout.setVisibility(View.VISIBLE);
        }
        errorImage.setImageResource(imageView);
        errorTitle.setText(title);
        errorMessage.setText(message);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoadSwipeRefresh("");
            }
        });
    }
}