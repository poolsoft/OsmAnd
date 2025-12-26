package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;

import java.lang.ref.WeakReference;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {

	public InAppPurchaseHelperImpl(OsmandApplication ctx) {
		super(ctx);
		purchases = new InAppPurchasesImpl(ctx);
		// Force activation on startup
		applyPurchases();
	}

	@Override public boolean isPurchasedLocalFullVersion() { return true; }
	@Override public boolean isPurchasedLocalDeepContours() { return true; }
	@Override public boolean isSubscribedToLocalLiveUpdates() { return true; }
	@Override public boolean isSubscribedToLocalOsmAndPro() { return true; }
	@Override public boolean isSubscribedToLocalMaps() { return true; }

	@Override
	public void isInAppPurchaseSupported(@NonNull final Activity activity, @Nullable final InAppPurchaseInitCallback callback) {
		if (callback != null) callback.onSuccess();
	}

	@NonNull
	@Override
	public String getPlatform() {
		return PLATFORM_GOOGLE; // Pretend we are Google platform to satisfy checks
	}

	@Override
	protected void execImpl(@NonNull final InAppPurchaseTaskType taskType, @NonNull final InAppCommand runnable) {
		// Mock implementation: just run the command, skipping billing logic
		if (runnable != null) {
			runnable.run(this);
		}
	}

	@Override
	public void purchaseFullVersion(@NonNull final Activity activity) {
		// Mock purchase success
	}

	@Override
	public void purchaseDepthContours(@NonNull final Activity activity) {
		// Mock purchase success
	}

	@Override
	public void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException {
		// Mock purchase success
	}

	@Override
	public void manageSubscription(@NonNull Context ctx, @Nullable String sku, @Nullable PurchaseOrigin origin) {
		// No-op for mock
	}

	@Override
	protected InAppCommand getPurchaseSubscriptionCommand(final WeakReference<Activity> activity, final String sku, final String userInfo) {
		return new InAppCommand() {
			@Override
			public void run(@NonNull InAppPurchaseHelper helper) {
				commandDone();
			}
		};
	}

	@Override
	protected InAppCommand getRequestInventoryCommand(boolean userRequested) {
		return new InAppCommand() {
			@Override
			protected boolean userRequested() {
				return userRequested;
			}
			@Override
			public void run(@NonNull InAppPurchaseHelper helper) {
				// Immediately verify everything is purchased
				applyPurchases();
				commandDone();
			}
		};
	}

	@Override
	protected boolean isBillingManagerExists() {
		return true; // Pretend it exists
	}

	@Override
	protected void destroyBillingManager() {
		// No-op
	}
}
