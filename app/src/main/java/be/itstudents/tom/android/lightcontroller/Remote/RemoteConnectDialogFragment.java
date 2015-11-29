package be.itstudents.tom.android.lightcontroller.Remote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import be.itstudents.tom.android.lightcontroller.MainActivity;
import be.itstudents.tom.android.lightcontroller.R;

public class RemoteConnectDialogFragment extends android.support.v4.app.DialogFragment {

    private Button mConnect;
    private EditText mHost;
    private EditText mPort;

    public static RemoteConnectDialogFragment newInstance() {
        RemoteConnectDialogFragment f = new RemoteConnectDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        // args.putInt("num", num);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.remote_connect_layout, null);
        mHost = (EditText) v.findViewById(R.id.remote_connect_host);
        mHost.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("remote_connect_host", ""));
        mPort = (EditText) v.findViewById(R.id.remote_connect_port);
        mPort.setText(Integer.toString(PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("remote_connect_port", (RemoteConstants.DEFAULT_PORT))));
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.remote_connect_title)
                .setView(v)
                .setPositiveButton(R.string.remote_connect_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                ((MainActivity) getActivity())
                                        .connect(mHost.getText().toString(), Integer.parseInt(mPort.getText().toString()));
                            }
                        })
                .setNegativeButton(R.string.remote_connect_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                dismiss();
                            }
                        }).create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


}
