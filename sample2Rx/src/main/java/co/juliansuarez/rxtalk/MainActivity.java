package co.juliansuarez.rxtalk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.juliansuarez.rxtalk.models.Item;
import co.juliansuarez.rxtalk.models.RepoSearchResults;
import co.juliansuarez.rxtalk.network.GithubApi;
import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.widget.OnTextChangeEvent;
import rx.android.widget.WidgetObservable;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public static final int MIN_CHARACTERS = 3;
    public static final int MIN_SECONDS = 2;
    RecyclerView recyclerView;
    ProgressBar progressBar;
    EditText editTextSearchTerm;
    private ItemAdapter itemAdapter;
    private GithubApi githubApi;
    private Subscription searchResultsSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        final Observable<OnTextChangeEvent> searchTermChangedObservable = WidgetObservable.text(editTextSearchTerm);
        final Observable<RepoSearchResults> searchResultsObservable = AppObservable.bindActivity(this,
                searchTermChangedObservable.debounce(MIN_SECONDS, TimeUnit.SECONDS)
                        .filter(onTextChangeEvent -> onTextChangeEvent.text().length() > MIN_CHARACTERS)
                        .flatMap(onTextChangeEvent -> callSearchWS(onTextChangeEvent.text().toString())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())));
        searchResultsSubscription = searchResultsObservable
                .subscribe(new Subscriber<RepoSearchResults>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(MainActivity.class.getSimpleName(), "Error", e);
                    }

                    @Override
                    public void onNext(RepoSearchResults repoSearchResults) {
                        showData(repoSearchResults.getItems());
                    }
                });
    }

    private void init() {
        editTextSearchTerm = (EditText) findViewById(R.id.editTextSearchTerm);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(itemAdapter);

        githubApi = new RestAdapter.Builder().setEndpoint("https://api.github.com").build().create(GithubApi.class);
    }

    private Observable<RepoSearchResults> callSearchWS(String searchTerm) {
        runOnUiThread(this::showProgressBar);
        return githubApi.searchRepos(searchTerm);
    }

    private void showProgressBar() {
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void showData(List<Item> repos) {
        itemAdapter.updateData(repos);
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        searchResultsSubscription.unsubscribe();
        super.onDestroy();
    }
}
