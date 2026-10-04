/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonIOException
 *  com.google.gson.JsonSyntaxException
 */
package code.wiosvisual.utils.client.managers.file.impl;

import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.exception.FileLoadException;
import code.wiosvisual.utils.client.managers.file.exception.FileSaveException;
import code.wiosvisual.utils.client.managers.file.impl.account.Account;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

public class AccountFile
extends ClientFile {
    private final AccountRepository accountRepository;

    public AccountFile(AccountRepository accountRepository) {
        super("accounts");
        this.accountRepository = accountRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, this.getName() + ".json");
        try (FileWriter writer = new FileWriter(file);){
            gson.toJson(this.accountRepository.accountList, (Appendable)writer);
        }
        catch (JsonIOException | IOException e) {
            throw new FileSaveException("Failed to save accounts to file", e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, this.getName() + ".json");
        try (FileReader reader = new FileReader(file);){
            Account[] accounts = (Account[])gson.fromJson((Reader)reader, Account[].class);
            this.accountRepository.accountList.clear();
            this.accountRepository.accountList.addAll(Arrays.asList(accounts));
        }
        catch (IOException e) {
            throw new FileLoadException("Failed to load accounts from file", e);
        }
        catch (JsonSyntaxException e) {
            throw new FileLoadException("JSON syntax error, accounts config cannot be loaded", e);
        }
        catch (JsonIOException e) {
            throw new FileLoadException("JSON IO error, accounts config cannot be loaded", e);
        }
    }
}
