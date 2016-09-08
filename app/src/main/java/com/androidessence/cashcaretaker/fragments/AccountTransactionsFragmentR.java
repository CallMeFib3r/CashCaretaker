package com.androidessence.cashcaretaker.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidessence.cashcaretaker.R;
import com.androidessence.cashcaretaker.adapters.TransactionAdapter;
import com.androidessence.cashcaretaker.core.CoreRecyclerViewFragment;
import com.androidessence.cashcaretaker.data.CCContract;

import java.text.MessageFormat;

/**
 * Displays a list of transactions for an account.
 *
 * Created by adam.mcneilly on 9/8/16.
 */
public class AccountTransactionsFragmentR extends CoreRecyclerViewFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String FRAGMENT_TAG = "AccountTransactionsFragment";

    // UI Elements
    private FloatingActionButton mAddTransactionFAB;
    private TransactionAdapter mTransactionAdapter;
    private TextView mAddFirstTransaction;
    private TextView mAccountBalanceTextView;

    // Format to display header
    private MessageFormat mAccountBalanceFormat;

    // Account we are showing transactions for.
    private long mAccount;
    private static final String ARG_ACCOUNT = "accountArg";
    private static final int TRANSACTION_LOADER = 0;
    private static final int ACCOUNT_BALANCE_LOADER = 1;

    private static final String[] ACCOUNT_BALANCE_COLUMNS = new String[] {
            CCContract.AccountEntry.COLUMN_BALANCE
    };

    private static final int ACCOUNT_BALANCE_INDEX = 0;

    public static AccountTransactionsFragment newInstance(long account){
        Bundle args = new Bundle();
        args.putLong(ARG_ACCOUNT, account);

        AccountTransactionsFragment fragment = new AccountTransactionsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccount = getArguments().getLong(ARG_ACCOUNT, 0);

        mAccountBalanceFormat = new MessageFormat(getActivity().getString(R.string.account_balance));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_transactions, container, false);

        getElements(root);
        setupRecyclerView(LinearLayoutManager.VERTICAL);
        setClickListeners();

        return root;
    }

    @Override
    protected void setupRecyclerView(int orientation) {
        super.setupRecyclerView(orientation);

        mTransactionAdapter = new TransactionAdapter(getActivity());
        recyclerView.setAdapter(mTransactionAdapter);
    }

    @Override
    protected void getElements(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.transaction_recycler_view);
        mAddTransactionFAB = (FloatingActionButton) view.findViewById(R.id.add_transaction_fab);
        mAddFirstTransaction = (TextView) view.findViewById(R.id.add_first_transaction_text_view);
        mAccountBalanceTextView = (TextView) view.findViewById(R.id.account_balance_header);
    }

    /**
     * Sets any necessary click listeners in the fragment.
     */
    private void setClickListeners(){
        mAddTransactionFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((OnAddTransactionFABClickedListener)getActivity()).addTransactionFABClicked();
            }
        });
    }

    /**
     * Restarts the loader whenever the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(TRANSACTION_LOADER, null, this);
        getLoaderManager().restartLoader(ACCOUNT_BALANCE_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id){
            case TRANSACTION_LOADER:
                return new CursorLoader(
                        getActivity(),
                        CCContract.TransactionEntry.buildTransactionsForAccountUri(mAccount),
                        TransactionAdapter.TRANSACTION_COLUMNS,
                        null,
                        null,
                        CCContract.TransactionEntry.COLUMN_DATE + " DESC"
                );
            case ACCOUNT_BALANCE_LOADER:
                return new CursorLoader(
                        getActivity(),
                        CCContract.AccountEntry.buildAccountUri(mAccount),
                        ACCOUNT_BALANCE_COLUMNS,
                        null,
                        null,
                        null
                );
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    //TODO: Abstract out the adapter to the base class too, making this simpler.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()){
            case TRANSACTION_LOADER:
                mTransactionAdapter.swapCursor(data);
                // If the cursor is empty hide the recyclerview.
                recyclerView.setVisibility(data.getCount() == 0 ? View.GONE : View.VISIBLE);
                // If the cursor is empty show the textview.
                mAddFirstTransaction.setVisibility(data.getCount() == 0 ? View.VISIBLE : View.GONE);
                break;
            case ACCOUNT_BALANCE_LOADER:
                double accountBalance = 0;

                if(data.moveToFirst()) {
                    accountBalance = data.getDouble(ACCOUNT_BALANCE_INDEX);
                }

                mAccountBalanceTextView.setText(mAccountBalanceFormat.format(new Object[] {accountBalance}));
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch(loader.getId()){
            case TRANSACTION_LOADER:
                mTransactionAdapter.swapCursor(null);
                break;
            case ACCOUNT_BALANCE_LOADER:
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    public void restartAccountBalanceLoader() {
        getLoaderManager().restartLoader(ACCOUNT_BALANCE_LOADER, null, this);
    }

    public interface OnAddTransactionFABClickedListener {
        void addTransactionFABClicked();
    }
}
