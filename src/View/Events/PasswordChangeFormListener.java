package View.Events;

import Model.User;
import View.PasswordChangeForm;

/**
 * Maneja los eventos de {@link PasswordChangeForm}: valida que los tres
 * campos estén completos, que la clave actual coincida con la real y que
 * la clave nueva y su confirmación sean iguales, y pide confirmación
 * antes de aplicar el cambio.
 */
public final class PasswordChangeFormListener {

    private final PasswordChangeForm view;
    private final User user;

    public PasswordChangeFormListener(PasswordChangeForm view, User user) {
        this.view = view;
        this.user = user;
        wire();
    }

    private void wire() {
        view.getConfirmButton().addActionListener(e -> onConfirm());
        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    private void onConfirm() {
        String current = new String(view.getCurrentPassword());
        String newPassword = new String(view.getNewPassword());
        String confirmed = new String(view.getConfirmedPassword());

        if (current.isEmpty() || newPassword.isEmpty() || confirmed.isEmpty()) {
            DialogHelper.warn(view, "Debe completar todos los campos para cambiar la clave.");
            return;
        }
        if (!current.equals(user.getPassword())) {
            DialogHelper.error(view, "La clave actual ingresada no es correcta.");
            return;
        }
        if (!newPassword.equals(confirmed)) {
            DialogHelper.warn(view, "La clave nueva y su confirmación no coinciden.");
            return;
        }
        if (!DialogHelper.confirm(view, "¿Desea confirmar el cambio de clave?")) {
            return;
        }

        user.setPassword(newPassword);
        DialogHelper.info(view, "Cambio de clave", "La clave se actualizó correctamente.");
        view.dispose();
    }
}
