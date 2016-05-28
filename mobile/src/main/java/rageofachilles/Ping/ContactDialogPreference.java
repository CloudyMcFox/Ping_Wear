package rageofachilles.Ping;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;

/**
 * Created by Ryan on 5/15/2016.
 */
public class ContactDialogPreference extends DialogPreference
{
    private final Uri QUERY_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    private final String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    private final String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
    private final String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
    private final int MAX_STRING = 20;
    private EditText m_etNumberView;
    private SearchView m_etSearchView;
    private String m_selectedPhoneNumber;
    private ArrayList<Contact> m_contactList = null;
    private ArrayList<Contact> m_contactListSearchable = null;
    private ListView m_lv = null;

    public ContactDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ContactDialogPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(!positiveResult) { // cancel button
            return;
        }
        String number = m_etNumberView.getText().toString();
        if (null == number || number.equals("")) {
            return;
        }

        if (null != m_selectedPhoneNumber && !m_selectedPhoneNumber.equals("")) {
            ((SettingsActivity)this.getContext()).Update("phoneNumber", m_selectedPhoneNumber);
        } else {// null is expected when number is entered manually
            ((SettingsActivity)this.getContext()).Update("phoneNumber", number);
        }
        ((SettingsActivity)this.getContext()).Update("phoneNumberText", number);
        super.onDialogClosed(positiveResult);
    }

    private void init(Context context) {
        setPersistent(false);
        setDialogLayoutResource(R.layout.contacts_layout);
    }

    public ArrayList<Contact> getContacts()
    {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = new String[]{ DISPLAY_NAME, HAS_PHONE_NUMBER, Phone.TYPE, PHONE_NUMBER};
        Cursor cursor = cr.query(QUERY_URI, projection, ContactsContract.Contacts.HAS_PHONE_NUMBER, null,  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
        ArrayList<Contact> contacts = new ArrayList<>();

        String oldName = "";
        while (cursor.moveToNext()) {

            String name = (cursor.getString(cursor.getColumnIndex(DISPLAY_NAME))).trim();
            final int typeIndex = cursor.getColumnIndex(Phone.TYPE);
            final int numberIndex = cursor.getColumnIndex(PHONE_NUMBER);
            boolean isMultiple = false;
            int type = cursor.getInt(typeIndex);
            String phoneNumber = cursor.getString(numberIndex);

            if(name.equals(oldName)) {
                isMultiple = true;
            } else {
                oldName = name;
            }
            contacts.add(new Contact(name, phoneNumber, TypeToString(type), isMultiple));
        }
        cursor.close();
        return contacts;
    }

    protected String TypeToString(int type)
    {
        switch (type) {
            case Phone.TYPE_HOME:
                return "Home";
            case Phone.TYPE_MOBILE:
                return "Mobile";
            case Phone.TYPE_WORK:
                return "Work";
            case Phone.TYPE_FAX_HOME:
                return "Home Fax";
            case Phone.TYPE_FAX_WORK:
                return "Work Fax";
            case Phone.TYPE_MAIN:
                return "Main";
            case Phone.TYPE_OTHER:
                return "Other";
            case Phone.TYPE_CUSTOM:
                return "Custom";
            case Phone.TYPE_PAGER:
                return "Pager";
            default:
                return "";
        }
    }

    public void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    protected void onBindDialogView(View view)
    {
        m_lv = ((ListView) view.findViewById(R.id.lvContact));
        m_contactList = getContacts();
        // copy contacts list to an editable one
        m_contactListSearchable = (ArrayList<Contact>)m_contactList.clone();

        m_etSearchView = ((SearchView) view.findViewById(R.id.etSearch));
        m_etNumberView = ((EditText) view.findViewById(R.id.etNumber)); //Save view to access it ondialogclose
        // Set the ArrayAdapter as the ListView's adapter.
        m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactListSearchable) );

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(!(v instanceof EditText)) { // don't hide keyboard if an text box was hit
                    hideSoftKeyboard(v);
                }
                return false;
            }
        });

        // Set the etNumberView to the current number
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String number = prefs.getString("phoneNumberText","");
        m_etNumberView.setText(number);

        m_etNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                * Commenting this out due not allowing inserting characters in front of text from contact.
                * instead just remove text
                // Highlight the text:
                // From Stack Overflow:
                // Problem caused by IME. If showed cursor drag pointer then selection must be zero width.
                // You need cancel drag pointer. It can be done by change text.
                // We replace first symbol of text with same symbol. It cause cancel drag pointer and allow make selection without bug.
                etNumberView.selectAll();
                if (etNumberView.getText().length() > 0) {
                    etNumberView.getText().replace(0, 1, etNumberView.getText().subSequence(0, 1), 0, 1);
                    etNumberView.selectAll();
                }
                */
                m_etNumberView.setText("");
            }
        });
        m_etNumberView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    // hack hack... clear selectedPhoneNumber to indicate to OnDialogClose that a contact was not click, but number entered manually
                    m_selectedPhoneNumber = null;
                    onDialogClosed(true); // fake dialog close to update value.
                    getDialog().dismiss();
                    return true;
                }
                return false;
            }
        });

        m_etSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_etSearchView.onActionViewExpanded(); // allow whole text box to trigger search (not just far left)
            }
        });

        m_etSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                m_contactListSearchable = (ArrayList<Contact>)m_contactList.clone();
                // Update contacts
                if(newText.equals("")){
                    m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactList));
                    return true;
                }

                for (int i =0; i < m_contactListSearchable.size(); i++) {
                    Contact cnt = m_contactListSearchable.get(i);
                    if (!cnt.getName().toLowerCase().contains(newText.toLowerCase())){ //if name does not match
                        String searchString = newText.toLowerCase().replaceAll("[\\D]",""); // for number check, remove all non digits
                        if (searchString.equals("") || !cnt.getNumber().toLowerCase().replaceAll("[\\D]","").contains(searchString)){ // if number doesn't match
                            m_contactListSearchable.remove(i);
                            // since we removed one, lower i again
                            i--;
                        }
                    }
                }
                // Unfortunately this now broke the hidden names as a phone number search may have hit on a hidden name. Need to re loop through
                // Optimization: merge with loop above.
                String oldName = "";
                for (int i =0; i < m_contactListSearchable.size(); i++) {
                    String newName = m_contactListSearchable.get(i).getName();
                    if (0 == i) {
                        // set first one to true always
                        m_contactListSearchable.get(i).setIsMultiple(false);
                        oldName = newName;
                        continue;
                    }
                    m_contactListSearchable.get(i).setIsMultiple(oldName.equals(newName) ? true: false);
                    oldName = newName;
                }

                m_lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, m_contactListSearchable));
                return true;
            }
            public boolean onQueryTextSubmit(String query) {
                m_etSearchView.clearFocus();
                return true;
            }
        });

        super.onBindDialogView(view);
    }

    public class ContactAdapter extends ArrayAdapter<Contact>
    {
        private final Context m_context;
        private final ArrayList<Contact> m_data;
        private final int m_layoutResourceId;

        public ContactAdapter(Context context, int layoutResourceId, ArrayList<Contact> data) {
            super(context, layoutResourceId, data);
            this.m_context = context;
            this.m_data = data;
            this.m_layoutResourceId = layoutResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if(row == null) {
                LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
                row = inflater.inflate(m_layoutResourceId, parent, false);

                holder = new ViewHolder();
                holder.textView1 = (TextView)row.findViewById(R.id.lblName);
                holder.textView2 = (TextView)row.findViewById(R.id.lblType);
                holder.textView3 = (TextView)row.findViewById(R.id.lblNumber);
                row.setTag(holder);
            } else {
                holder = (ViewHolder)row.getTag();
            }
            final View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove keyboard if it exists
                    hideSoftKeyboard(v);
                    // first get the name
                    String number = ((TextView)v.findViewById(R.id.lblNumber)).getText().toString();
                    m_selectedPhoneNumber = number; // Save number to store in preferences on close
                    String name = ((TextView)v.findViewById(R.id.lblName)).getText().toString();
                    View parent = (View)v.getParent().getParent();// Contacts is first parent, Dialog is second.
                    EditText et = (EditText)parent.findViewById(R.id.etNumber);
                    if (null != et){
                        et.setText(name+ "- " + number);
                    }
                }
            };
            row.setOnClickListener(onClickListener);

            Contact person = m_data.get(position);

            String name = person.getName();
            if (name.length() > MAX_STRING) {
                name = name.substring(0, MAX_STRING);
                name = name.concat("...");
            }
            holder.textView1.setText(name);
            if ( person.getIsMultiple()){
                holder.textView1.setVisibility(View.INVISIBLE);
            } else {
                holder.textView1.setVisibility(View.VISIBLE);
            }
            holder.textView2.setText(person.getType());
            holder.textView3.setText(person.getNumber());
            return row;
        }

        class ViewHolder
        {
            TextView textView1;
            TextView textView2;
            TextView textView3;
        }
    }


    public class Contact
    {
        private String m_name;
        private String m_number;
        private String m_type;
        boolean m_fIsMultiple = false; // Used to hide name label if its a multiple.

        public Contact (String input_name, String input_number, String input_type, boolean isMultiple) {
            m_name = input_name;
            m_number = input_number;
            m_type = input_type;
            m_fIsMultiple = isMultiple;
        }

        public String getName(){
            return m_name;
        }

        public String getNumber(){
            return m_number;
        }

        public String getType(){
            return m_type;
        }

        public boolean getIsMultiple(){
            return m_fIsMultiple;
        }

        public void setIsMultiple(boolean input){
            m_fIsMultiple = input;
        }
    }
}

