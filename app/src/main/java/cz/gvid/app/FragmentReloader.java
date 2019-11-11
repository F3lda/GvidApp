package cz.gvid.app;

@SuppressWarnings("WeakerAccess")
public class FragmentReloader {

    private OnNeedReloadFragmentListener mOnNeedReloadFragmentListener;

    public void setOnNeedReloadFragmentListener(OnNeedReloadFragmentListener reloaderListener)
    {
        mOnNeedReloadFragmentListener = reloaderListener;
    }

    public void reloadFragment(String reloadFragment)
    {
        if(mOnNeedReloadFragmentListener != null)
        {
            mOnNeedReloadFragmentListener.onReloadFragment(reloadFragment);
        }
    }
}
