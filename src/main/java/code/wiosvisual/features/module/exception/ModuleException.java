/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.module.exception;

public final class ModuleException
extends RuntimeException {
    private final String message;
    private final String moduleName;

    public ModuleException(String message, String moduleName) {
        this.message = message;
        this.moduleName = moduleName;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    @Override
    public String toString() {
        return "ModuleException(message=" + this.getMessage() + ", moduleName=" + this.getModuleName() + ")";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ModuleException)) {
            return false;
        }
        ModuleException other = (ModuleException)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        String this$moduleName = this.getModuleName();
        String other$moduleName = other.getModuleName();
        return !(this$moduleName == null ? other$moduleName != null : !this$moduleName.equals(other$moduleName));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ModuleException;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        String $moduleName = this.getModuleName();
        result = result * 59 + ($moduleName == null ? 43 : $moduleName.hashCode());
        return result;
    }
}
