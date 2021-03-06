package org.lndroid.messenger;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.lndroid.framework.DefaultIpcCodecProvider;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.PluginClientBuilder;

public class WalletViewModelBase extends AndroidViewModel {

    private static final String TAG = "WalletViewModelBase";

    private Context ctx_;
    private Database db_;
    private WalletServiceDao walletServiceDao_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();
    private MutableLiveData<Boolean> ready_ = new MutableLiveData<>();
    private IPluginClient pluginClient_;

    protected Database db() {
        return db_;
    }

    protected WalletServiceDao walletServiceDao() {
        return walletServiceDao_;
    }

    protected IPluginClient pluginClient() {
        return pluginClient_;
    }

    protected void onConnect() {
        // init use cases in subclasses
    }

    public WalletViewModelBase(Application app) {
        super(app);

        ctx_ = app;

        db_ = Database.open(ctx_);

        walletServiceDao_ = db_.walletServiceDao();

        walletService_.observeForever(new Observer<WalletService>() {
            @Override
            public void onChanged(WalletService walletService) {
                if (ready_.getValue() == null) {
                    if (walletService != null)
                        connect();
                    ready_.setValue(walletService != null);
                }
            }
        });

        db_.execute(new Runnable() {
            @Override
            public void run() {
                WalletService ws = walletServiceDao_.getWalletService();
                walletService_.postValue(ws);
            }
        });
    }

    void connect() {
        WalletService ws = walletService_.getValue();

        pluginClient_ = new PluginClientBuilder()
                .setIpc(true)
                .setIpcCodecProvider(new DefaultIpcCodecProvider())
                .setUserIdentity(WalletData.UserIdentity.builder()
                        .setAppPubkey(WalletKeyStore.getInstance().getAppPubkey())
                        .setAppPackageName(Constants.APP_PACKAGE_NAME)
                        .build())
                .setServicePackageName(ws.packageName)
                .setServiceClassName(ws.className)
                .setServicePubkey(ws.pubkey)
                .build();
        pluginClient_.connect(ctx_);

        onConnect();
    }

    LiveData<Boolean> ready() {
        return ready_;
    }
}



