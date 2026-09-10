package edu.cnu.ced.view3d;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.opengl.GLProfile;

import edu.cnu.mdi.log.Log;

/**
 * Warms up JOGL's {@link GLProfile} singleton on a background thread, off
 * the Swing event-dispatch thread.
 *
 * <p>
 * JOGL's first-ever GL context / shared-resource creation on macOS makes a
 * synchronous cross-thread call to the AppKit main thread ({@code
 * jogamp.nativewindow.macosx.OSXUtil.RunOnMainThread}). If that first call
 * happens on the EDT -- which it otherwise would, from the first {@code
 * Panel3D}'s own {@code GLProfile.isAvailable()} call, the moment a user
 * opens the first 3D view -- and the AppKit main thread happens to be
 * mid-callback into the EDT at that exact moment (observed in practice:
 * servicing a macOS accessibility query), the two threads wait on each
 * other forever. A real deadlock (the "spinning color wheel of death"),
 * not a slow paint, with no exception for {@code Panel3D}'s own broad
 * {@code catch (Throwable)} to see.
 * </p>
 *
 * <p>
 * {@link #start()} should be called once, as early as possible in {@code
 * main()} -- well before any 3D view can possibly be realized -- so the
 * several seconds of unrelated startup work (magnetic fields, geometry)
 * that already run at that point give this a wide head start. {@link
 * CedView3D} calls {@link #awaitReady(long)} with a generous bound before
 * its {@code PlainView3D}/{@code Panel3D} superclass chain ever runs, so
 * even a 3D view opened unusually early during startup still waits for
 * the warm-up to finish rather than racing it on the EDT.
 * </p>
 */
public final class GLWarmup {

	private static final AtomicBoolean started = new AtomicBoolean();
	private static final CountDownLatch done = new CountDownLatch(1);

	private GLWarmup() {
	}

	/**
	 * Starts the background warm-up. Safe to call more than once; only the
	 * first call does anything. Called once, from {@code CedApplication.
	 * main()}, as early as possible.
	 */
	public static void start() {
		if (!started.compareAndSet(false, true)) {
			return;
		}
		Thread warmup = new Thread(() -> {
			try {
				long begin = System.nanoTime();
				GLProfile.initSingleton();
				Log.getInstance().config("Startup timing - GLProfile warm-up: "
						+ (System.nanoTime() - begin) / 1_000_000 + " ms");
			} catch (Throwable t) {
				// 3D views fall back to an "unavailable" panel on a real GL
				// failure (see Panel3D's own constructor); nothing more to
				// do here than note it -- the countDown() below still runs,
				// so a failed warm-up cannot itself hang view construction.
				Log.getInstance().config("GLProfile warm-up failed (3D views will report unavailable): " + t);
			} finally {
				done.countDown();
			}
		}, "GLProfile-warmup");
		warmup.setDaemon(true);
		warmup.start();
	}

	/**
	 * Blocks the calling thread (expected: the EDT, briefly) until the
	 * warm-up started by {@link #start()} has finished, {@code
	 * timeoutMillis} has elapsed, or {@link #start()} was never called (in
	 * which case this returns immediately -- e.g. in a standalone test that
	 * constructs a 3D view directly, without going through {@code
	 * CedApplication.main()}).
	 */
	static void awaitReady(long timeoutMillis) {
		if (!started.get()) {
			return;
		}
		try {
			done.await(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
