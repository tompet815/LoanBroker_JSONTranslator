package models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ReceiveData extends Data implements Serializable {

    private List<String> bankExchangeNames;

    public ReceiveData(String ssn, int creditScore, double loanAmoount, int loanDuration, List<String> bankExchangeNames) {
        super(ssn, creditScore, loanAmoount, loanDuration);
        this.bankExchangeNames = bankExchangeNames;
    }

    public List<String> getBankExchangeNames() {
        return bankExchangeNames;
    }

    @Override
    public String toString() {
        return "ReceiveData{" + "bankExchangeNames=" + bankExchangeNames + "ssn=" + getSsn() + ", creditScore=" + getCreditScore() + ", loanAmoount=" + getLoanAmoount() + ", loanDuration=" + getLoanDuration() + '}';
    }

}
